package com.example.androidautoytstreamer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentQueueIndex = -1

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
        fun getPlayer(): ExoPlayer? = player
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
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
    }

    fun playCurrent() {
        if (player?.mediaItemCount == 0) return
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        if (player?.mediaItemCount == 0) return
        player?.play()
    }

    fun next(): PlaylistVideo? {
        val nextIndex = findNextPlayableIndex(currentQueueIndex + 1)
            ?: return null
        currentQueueIndex = nextIndex
        player?.seekToDefaultPosition(nextIndex)
        player?.play()
        return queue[nextIndex]
    }

    fun previous(): PlaylistVideo? {
        val previousIndex = findPreviousPlayableIndex(currentQueueIndex - 1)
            ?: return null
        currentQueueIndex = previousIndex
        player?.seekToDefaultPosition(previousIndex)
        player?.play()
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
        if (currentQueueIndex !in queue.indices) return
        val current = queue[currentQueueIndex]
        queue[currentQueueIndex] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
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
