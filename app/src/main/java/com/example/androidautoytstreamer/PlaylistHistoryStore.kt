package com.example.androidautoytstreamer

import java.util.concurrent.ConcurrentHashMap

private data class HistoryEntry(
    val status: WatchStatus,
    val resumeAtSeconds: Int
)

class PlaylistHistoryStore {
    private val history = ConcurrentHashMap<String, HistoryEntry>()

    fun applyToQueue(queue: List<PlaylistVideo>): List<PlaylistVideo> {
        return queue.map { video ->
            val entry = history[video.id] ?: return@map video

            when (entry.status) {
                WatchStatus.COMPLETED -> video.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
                WatchStatus.IN_PROGRESS -> video.copy(status = WatchStatus.IN_PROGRESS, resumeAtSeconds = entry.resumeAtSeconds)
                WatchStatus.NOT_STARTED -> video.copy(status = WatchStatus.NOT_STARTED, resumeAtSeconds = 0)
            }
        }
    }

    fun markStarted(videoId: String, resumeAtSeconds: Int = 0) {
        history[videoId] = HistoryEntry(WatchStatus.IN_PROGRESS, resumeAtSeconds)
    }

    fun markPaused(videoId: String, resumeAtSeconds: Int) {
        history[videoId] = HistoryEntry(WatchStatus.IN_PROGRESS, resumeAtSeconds)
    }

    fun markCompleted(videoId: String) {
        history[videoId] = HistoryEntry(WatchStatus.COMPLETED, 0)
    }

    fun getResumeSeconds(videoId: String): Int {
        return history[videoId]?.resumeAtSeconds ?: 0
    }

    fun hasCompleted(videoId: String): Boolean {
        return history[videoId]?.status == WatchStatus.COMPLETED
    }
}

