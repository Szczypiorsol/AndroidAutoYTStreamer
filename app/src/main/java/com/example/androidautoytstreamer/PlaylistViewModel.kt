package com.example.androidautoytstreamer

import androidx.lifecycle.ViewModel

class PlaylistViewModel : ViewModel() {
    private val queueManager = PlaylistQueueManager()
    private val repository = YouTubePlaylistRepository()

    fun loadPlaylist(videos: List<PlaylistVideo>) {
        queueManager.setQueue(videos)
    }

    fun loadFromPlaylistInput(rawInput: String): Result<List<PlaylistVideo>> {
        val result = repository.loadPlaylistFromUrl(rawInput)
        result.getOrNull()?.let { queueManager.setQueue(it) }
        return result
    }

    fun currentVideo(): PlaylistVideo? = queueManager.current()

    fun nextVideo(): PlaylistVideo? = queueManager.next()

    fun previousVideo(): PlaylistVideo? = queueManager.previous()

    fun markStarted(videoId: String) = queueManager.markStarted(videoId)

    fun markPaused(videoId: String, secondsWatched: Int) = queueManager.markPaused(videoId, secondsWatched)

    fun markCompleted(videoId: String) = queueManager.markCompleted(videoId)

    fun remainingVideos(): List<PlaylistVideo> = queueManager.remainingVideos()

    fun snapshot(): List<PlaylistVideo> = queueManager.snapshot()
}
