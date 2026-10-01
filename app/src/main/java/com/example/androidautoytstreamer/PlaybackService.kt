package com.example.androidautoytstreamer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentQueueIndex = -1
    private var lastKnownQueueIndex = -1

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
                        currentQueueIndex = nextPlayableIndex
                        localPlayer.seekToDefaultPosition(nextPlayableIndex)
                        localPlayer.play()
                    } else {
                        localPlayer.pause()
                    }
                } else {
                    markCurrentStarted()
                }
            }

            lastKnownQueueIndex = currentQueueIndex
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                markCurrentStarted()
            } else {
                saveCurrentProgress()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
        fun getPlayer(): ExoPlayer? = player
    }

    override fun onCreate() {
        super.onCreate()
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

        if (queue.isEmpty()) {
            player?.clearMediaItems()
            currentQueueIndex = -1
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
    }

    fun playCurrent() {
        if (player?.mediaItemCount == 0) return
        player?.play()
    }

    fun pause() {
        saveCurrentProgress()
        player?.pause()
    }

    fun resume() {
        if (player?.mediaItemCount == 0) return
        player?.play()
    }

    fun next(): PlaylistVideo? {
        saveCurrentProgress()
        val nextIndex = findNextPlayableIndex(currentQueueIndex + 1)
            ?: return null
        currentQueueIndex = nextIndex
        lastKnownQueueIndex = nextIndex
        player?.seekToDefaultPosition(nextIndex)
        player?.play()
        markCurrentStarted()
        return queue[nextIndex]
    }

    fun previous(): PlaylistVideo? {
        saveCurrentProgress()
        val previousIndex = findPreviousPlayableIndex(currentQueueIndex - 1)
            ?: return null
        currentQueueIndex = previousIndex
        lastKnownQueueIndex = previousIndex
        player?.seekToDefaultPosition(previousIndex)
        player?.play()
        markCurrentStarted()
        return queue[previousIndex]
    }

    fun currentVideo(): PlaylistVideo? {
        return if (currentQueueIndex in queue.indices) queue[currentQueueIndex] else null
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
    }

    private fun markCompletedAt(index: Int) {
        if (index !in queue.indices) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
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
