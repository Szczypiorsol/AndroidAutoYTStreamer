package com.example.androidautoytstreamer

import androidx.lifecycle.ViewModel

class PlaylistViewModel(
    private val queueManager: PlaylistQueueManager = PlaylistQueueManager(),
    private val loadPlaylistFromInput: (String) -> Result<List<PlaylistVideo>> = YouTubePlaylistRepository()::loadPlaylistFromUrl
) : ViewModel() {

    fun loadPlaylist(videos: List<PlaylistVideo>) {
        queueManager.setQueue(videos)
    }

    fun loadFromPlaylistInput(rawInput: String): Result<List<PlaylistVideo>> {
        val result = loadPlaylistFromInput(rawInput)
        result.getOrNull()?.let { queueManager.setQueue(it) }
        return result
    }

    fun currentVideo(): PlaylistVideo? = queueManager.current()

    fun nextVideo(): PlaylistVideo? = queueManager.next()

    fun previousVideo(): PlaylistVideo? = queueManager.previous()

    fun markStarted(videoId: String) = queueManager.markStarted(videoId)

    fun markPaused(videoId: String, secondsWatched: Int) = queueManager.markPaused(videoId, secondsWatched)


    fun remainingVideos(): List<PlaylistVideo> = queueManager.remainingVideos()

    fun snapshot(): List<PlaylistVideo> = queueManager.snapshot()
}
