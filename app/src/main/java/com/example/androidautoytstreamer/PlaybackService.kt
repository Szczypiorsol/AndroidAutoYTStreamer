package com.example.androidautoytstreamer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import java.util.concurrent.CopyOnWriteArraySet

data class PlaybackSnapshot(
    val currentVideo: PlaylistVideo? = null,
    val isPlaying: Boolean = false,
    val positionSeconds: Int = 0,
    val queueEnded: Boolean = false
)

fun interface PlaybackStateListener {
    fun onPlaybackStateChanged(snapshot: PlaybackSnapshot)
}

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentQueueIndex = -1
    private var lastKnownQueueIndex = -1
    private var queueEnded = false
    private val playbackListeners = CopyOnWriteArraySet<PlaybackStateListener>()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val localPlayer = player ?: return
            val newIndex = localPlayer.currentMediaItemIndex

            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                markCompletedAt(lastKnownQueueIndex)
            }

            if (newIndex in queue.indices) {
                currentQueueIndex = newIndex
                if (queue[newIndex].status == WatchStatus.COMPLETED) {
                    val nextPlayableIndex = findNextPlayableIndex(newIndex + 1)
                    if (nextPlayableIndex != null) {
                        queueEnded = false
                        currentQueueIndex = nextPlayableIndex
                        localPlayer.seekToDefaultPosition(nextPlayableIndex)
                        localPlayer.play()
                    } else {
                        endQueuePlayback()
                    }
                } else {
                    queueEnded = false
                    markCurrentStarted()
                }
            }

            lastKnownQueueIndex = currentQueueIndex
            notifyPlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                markCurrentStarted()
            } else {
                saveCurrentProgress()
            }
            notifyPlaybackState()
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
        fun getPlayer(): ExoPlayer? = player
    }

    override fun onCreate() {
        super.onCreate()
        PlaylistHistoryStore.initialize(filesDir)
        player = ExoPlayer.Builder(this).build()
        player?.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onDestroy() {
        saveCurrentProgress()
        player?.removeListener(playerListener)
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveCurrentProgress()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setQueue(videos: List<PlaylistVideo>) {
        queue.clear()
        queue.addAll(videos)
        queueEnded = false

        if (queue.isEmpty()) {
            player?.clearMediaItems()
            currentQueueIndex = -1
            notifyPlaybackState()
            return
        }

        val preferredIndex = queue.indexOfFirst { it.status == WatchStatus.IN_PROGRESS }
            .takeIf { it != -1 }
            ?: queue.indexOfFirst { it.status == WatchStatus.NOT_STARTED }
            ?: 0

        currentQueueIndex = preferredIndex
        lastKnownQueueIndex = preferredIndex
        val mediaItems = queue.map { video ->
            MediaItem.Builder()
                .setMediaId(video.id)
                .setUri("https://www.youtube.com/watch?v=${video.id}")
                .setTag(video.id)
                .build()
        }

        player?.setMediaItems(mediaItems, false)
        player?.prepare()
        player?.seekTo(preferredIndex, (queue[preferredIndex].resumeAtSeconds * 1000L).coerceAtLeast(0L))
        player?.playWhenReady = true
        player?.play()
        markCurrentStarted()
        notifyPlaybackState()
    }

    fun playCurrent() {
        if (player?.mediaItemCount == 0) return
        if (queueEnded) return
        player?.play()
    }

    fun pause() {
        saveCurrentProgress()
        player?.pause()
        notifyPlaybackState()
    }

    fun resume() {
        if (player?.mediaItemCount == 0) return
        if (queueEnded) return
        player?.play()
        notifyPlaybackState()
    }

    fun next(): PlaylistVideo? {
        saveCurrentProgress()
        val nextIndex = findNextPlayableIndex(currentQueueIndex + 1)
            ?: run {
                endQueuePlayback()
                return null
            }
        queueEnded = false
        currentQueueIndex = nextIndex
        lastKnownQueueIndex = nextIndex
        player?.seekToDefaultPosition(nextIndex)
        player?.play()
        markCurrentStarted()
        notifyPlaybackState()
        return queue[nextIndex]
    }

    fun previous(): PlaylistVideo? {
        saveCurrentProgress()
        val previousIndex = findPreviousPlayableIndex(currentQueueIndex - 1)
            ?: return null
        queueEnded = false
        currentQueueIndex = previousIndex
        lastKnownQueueIndex = previousIndex
        player?.seekToDefaultPosition(previousIndex)
        player?.play()
        markCurrentStarted()
        notifyPlaybackState()
        return queue[previousIndex]
    }

    fun currentVideo(): PlaylistVideo? {
        return if (currentQueueIndex in queue.indices) queue[currentQueueIndex] else null
    }

    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.add(listener)
        listener.onPlaybackStateChanged(currentPlaybackSnapshot())
    }

    fun removePlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.remove(listener)
    }

    fun currentPlaybackSnapshot(): PlaybackSnapshot {
        val localPlayer = player
        return PlaybackSnapshot(
            currentVideo = currentVideo(),
            isPlaying = localPlayer?.isPlaying == true,
            positionSeconds = ((localPlayer?.currentPosition ?: 0L) / 1000L).toInt().coerceAtLeast(0),
            queueEnded = queueEnded
        )
    }

    fun markCurrentStarted() {
        if (currentQueueIndex !in queue.indices) return
        val current = queue[currentQueueIndex]
        queue[currentQueueIndex] = current.copy(status = WatchStatus.IN_PROGRESS)
    }

    fun markCurrentCompleted() {
        markCompletedAt(currentQueueIndex)
    }

    private fun saveCurrentProgress() {
        val localPlayer = player ?: return
        if (currentQueueIndex !in queue.indices) return

        val current = queue[currentQueueIndex]
        if (current.status == WatchStatus.COMPLETED) return

        val resumeSeconds = (localPlayer.currentPosition / 1000L).toInt().coerceAtLeast(0)
        queue[currentQueueIndex] = current.copy(
            status = WatchStatus.IN_PROGRESS,
            resumeAtSeconds = resumeSeconds
        )
        notifyPlaybackState()
    }

    private fun markCompletedAt(index: Int) {
        if (index !in queue.indices) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
        notifyPlaybackState()
    }

    private fun notifyPlaybackState() {
        val snapshot = currentPlaybackSnapshot()
        playbackListeners.forEach { listener ->
            listener.onPlaybackStateChanged(snapshot)
        }
    }

    private fun endQueuePlayback() {
        queueEnded = true
        player?.pause()
        notifyPlaybackState()
    }

    private fun findNextPlayableIndex(fromIndex: Int): Int? {
        if (queue.isEmpty()) return null
        for (index in fromIndex until queue.size) {
            if (queue[index].status != WatchStatus.COMPLETED) return index
        }
        return null
    }

    private fun findPreviousPlayableIndex(fromIndex: Int): Int? {
        if (queue.isEmpty()) return null
        for (index in fromIndex downTo 0) {
            if (queue[index].status != WatchStatus.COMPLETED) return index
        }
        return null
    }
}
