package com.example.androidautoytstreamer

class PlaylistQueueManager {
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentIndex = -1
    private val historyStore = PlaylistHistoryStore()

    fun setQueue(videos: List<PlaylistVideo>) {
        queue.clear()
        queue.addAll(historyStore.applyToQueue(videos))
        currentIndex = queue.indexOfFirst { it.status == WatchStatus.IN_PROGRESS }
        if (currentIndex == -1) {
            currentIndex = queue.indexOfFirst { it.status == WatchStatus.NOT_STARTED }
        }
        if (currentIndex == -1 && queue.isNotEmpty()) {
            currentIndex = 0
        }
    }

    fun next(): PlaylistVideo? {
        if (queue.isEmpty()) return null
        val startIndex = if (currentIndex in queue.indices) currentIndex else 0
        for (index in startIndex + 1 until queue.size) {
            if (queue[index].status != WatchStatus.COMPLETED) {
                currentIndex = index
                return queue[currentIndex]
            }
        }
        return null
    }

    fun previous(): PlaylistVideo? {
        if (queue.isEmpty()) return null
        val startIndex = if (currentIndex in queue.indices) currentIndex else queue.size - 1
        for (index in startIndex - 1 downTo 0) {
            if (queue[index].status != WatchStatus.COMPLETED) {
                currentIndex = index
                return queue[currentIndex]
            }
        }
        return null
    }

    fun current(): PlaylistVideo? {
        if (queue.isEmpty() || currentIndex !in queue.indices) return null
        return queue[currentIndex]
    }

    fun markStarted(videoId: String) {
        val index = queue.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.IN_PROGRESS)
        historyStore.markStarted(videoId, current.resumeAtSeconds)
        currentIndex = index
    }

    fun markCompleted(videoId: String) {
        val index = queue.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
        historyStore.markCompleted(videoId)
        if (currentIndex == index) {
            currentIndex = nextIndexAfterCompletion(index)
        }
    }

    fun markPaused(videoId: String, secondsWatched: Int) {
        val index = queue.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.IN_PROGRESS, resumeAtSeconds = secondsWatched)
        historyStore.markPaused(videoId, secondsWatched)
    }

    fun remainingVideos(): List<PlaylistVideo> {
        return queue.filter { it.status != WatchStatus.COMPLETED }
    }

    fun snapshot(): List<PlaylistVideo> = queue.toList()

    private fun nextIndexAfterCompletion(startIndex: Int): Int {
        for (index in startIndex + 1 until queue.size) {
            if (queue[index].status != WatchStatus.COMPLETED) return index
        }
        return if (queue.isNotEmpty()) queue.size - 1 else -1
    }
}
