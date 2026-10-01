package com.example.androidautoytstreamer

data class PlaybackQueueSnapshot(
    val queue: List<PlaylistVideo> = emptyList(),
    val playback: PlaybackSnapshot = PlaybackSnapshot()
)

fun interface PlaybackQueueListener {
    fun onQueueSnapshotChanged(snapshot: PlaybackQueueSnapshot)
}

object PlaybackQueueBridge {
    @Volatile
    private var state = PlaybackQueueSnapshot()
    private val listeners = LinkedHashSet<PlaybackQueueListener>()

    @Synchronized
    fun update(queue: List<PlaylistVideo>, playback: PlaybackSnapshot) {
        state = PlaybackQueueSnapshot(queue = queue.toList(), playback = playback)
        val snapshot = state
        listeners.forEach { it.onQueueSnapshotChanged(snapshot) }
    }

    fun snapshot(): PlaybackQueueSnapshot = state

    @Synchronized
    fun addListener(listener: PlaybackQueueListener) {
        listeners.add(listener)
        listener.onQueueSnapshotChanged(state)
    }

    @Synchronized
    fun removeListener(listener: PlaybackQueueListener) {
        listeners.remove(listener)
    }
}

