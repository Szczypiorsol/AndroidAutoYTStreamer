package com.example.androidautoytstreamer

class PlaylistQueueManager {
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentIndex = -1

    fun setQueue(videos: List<PlaylistVideo>) {
        queue.clear()
        queue.addAll(videos)
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
        val candidateIndex = currentIndex + 1
        if (candidateIndex >= queue.size) return null
        currentIndex = candidateIndex
        return queue[currentIndex]
    }

    fun previous(): PlaylistVideo? {
        if (queue.isEmpty()) return null
        val candidateIndex = currentIndex - 1
        if (candidateIndex < 0) return null
        currentIndex = candidateIndex
        return queue[currentIndex]
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
        currentIndex = index
    }

    fun markCompleted(videoId: String) {
        val index = queue.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
        if (currentIndex == index) {
            currentIndex = minOf(index + 1, queue.size - 1)
        }
    }

    fun markPaused(videoId: String, secondsWatched: Int) {
        val index = queue.indexOfFirst { it.id == videoId }
        if (index == -1) return
        val current = queue[index]
        queue[index] = current.copy(status = WatchStatus.IN_PROGRESS, resumeAtSeconds = secondsWatched)
    }

    fun remainingVideos(): List<PlaylistVideo> {
        return queue.filter { it.status != WatchStatus.COMPLETED }
    }

    fun snapshot(): List<PlaylistVideo> = queue.toList()
}

