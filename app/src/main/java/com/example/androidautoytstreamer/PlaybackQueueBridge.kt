package com.example.androidautoytstreamer

data class PlaybackQueueSnapshot(
    val queue: List<PlaylistVideo> = emptyList(),
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val queueVersion: Long = 0L
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
        val queueCopy = queue.toList()
        val queueChanged = queueCopy != state.queue
        val nextVersion = if (queueChanged) state.queueVersion + 1L else state.queueVersion

        state = PlaybackQueueSnapshot(
            queue = queueCopy,
            playback = playback,
            queueVersion = nextVersion
        )
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

    @Synchronized
    internal fun resetForTests() {
        listeners.clear()
        state = PlaybackQueueSnapshot()
    }
}

