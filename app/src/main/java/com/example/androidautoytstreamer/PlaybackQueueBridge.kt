package com.example.androidautoytstreamer

data class PlaybackQueueSnapshot(
    val queue: List<PlaylistVideo> = emptyList(),
    val playback: PlaybackSnapshot = PlaybackSnapshot()
)

object PlaybackQueueBridge {
    @Volatile
    private var state = PlaybackQueueSnapshot()

    @Synchronized
    fun update(queue: List<PlaylistVideo>, playback: PlaybackSnapshot) {
        state = PlaybackQueueSnapshot(queue = queue.toList(), playback = playback)
    }

    fun snapshot(): PlaybackQueueSnapshot = state
}

