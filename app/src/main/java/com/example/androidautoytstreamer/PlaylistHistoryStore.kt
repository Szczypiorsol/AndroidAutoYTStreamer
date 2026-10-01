package com.example.androidautoytstreamer

import java.io.File
import java.util.concurrent.ConcurrentHashMap

private data class HistoryEntry(
    val status: WatchStatus,
    val resumeAtSeconds: Int
)

class PlaylistHistoryStore {
    private val history = sharedHistory

    companion object {
        private val sharedHistory = ConcurrentHashMap<String, HistoryEntry>()
        @Volatile
        private var storageFile: File? = null
        @Volatile
        private var loadedFromDisk = false
        private val storageLock = Any()

        fun initialize(filesDir: File) {
            synchronized(storageLock) {
                storageFile = File(filesDir, "playlist_history_state.txt")
                if (!loadedFromDisk) {
                    loadFromDiskLocked()
                    loadedFromDisk = true
                }
            }
        }

        internal fun clearInMemoryForTests() {
            synchronized(storageLock) {
                sharedHistory.clear()
                loadedFromDisk = false
            }
        }

        private fun loadFromDiskLocked() {
            val file = storageFile ?: return
            if (!file.exists()) return

            runCatching {
                file.readLines().forEach { line ->
                    val parts = line.split("\t")
                    if (parts.size != 3) return@forEach

                    val videoId = parts[0]
                    val status = runCatching { WatchStatus.valueOf(parts[1]) }.getOrNull() ?: return@forEach
                    val resumeAtSeconds = parts[2].toIntOrNull() ?: 0
                    sharedHistory[videoId] = HistoryEntry(status, resumeAtSeconds)
                }
            }
        }

        private fun persistToDiskLocked() {
            val file = storageFile ?: return

            runCatching {
                if (!file.exists()) {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                }

                val rows = sharedHistory.entries.joinToString(separator = "\n") { entry ->
                    "${entry.key}\t${entry.value.status.name}\t${entry.value.resumeAtSeconds}"
                }
                file.writeText(rows)
            }
        }
    }

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
        persistIfConfigured()
    }

    fun markPaused(videoId: String, resumeAtSeconds: Int) {
        history[videoId] = HistoryEntry(WatchStatus.IN_PROGRESS, resumeAtSeconds)
        persistIfConfigured()
    }

    fun markCompleted(videoId: String) {
        history[videoId] = HistoryEntry(WatchStatus.COMPLETED, 0)
        persistIfConfigured()
    }

    fun getResumeSeconds(videoId: String): Int {
        return history[videoId]?.resumeAtSeconds ?: 0
    }

    fun hasCompleted(videoId: String): Boolean {
        return history[videoId]?.status == WatchStatus.COMPLETED
    }

    private fun persistIfConfigured() {
        synchronized(storageLock) {
            if (loadedFromDisk) {
                persistToDiskLocked()
            }
        }
    }
}

