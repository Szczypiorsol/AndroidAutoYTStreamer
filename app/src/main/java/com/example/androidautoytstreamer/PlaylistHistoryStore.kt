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
        private var queueCacheFile: File? = null
        @Volatile
        private var loadedFromDisk = false
        private val storageLock = Any()
        private const val QUEUE_CACHE_TIMESTAMP_PREFIX = "#savedAt="

        fun initialize(filesDir: File) {
            synchronized(storageLock) {
                storageFile = File(filesDir, "playlist_history_state.txt")
                queueCacheFile = File(filesDir, "playlist_queue_cache.txt")
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
                AppLog.d("PlaylistHistoryStore: loaded ${sharedHistory.size} entries from disk")
            }.onFailure {
                AppLog.e("PlaylistHistoryStore: failed to load from disk", it)
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
            }.onFailure {
                AppLog.e("PlaylistHistoryStore: failed to persist to disk", it)
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

    fun saveLastMiniQueue(queue: List<PlaylistVideo>, limit: Int = 40) {
        synchronized(storageLock) {
            val file = queueCacheFile ?: return

            runCatching {
                if (!file.exists()) {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                }

                val rows = queue.take(limit).joinToString(separator = "\n") { video ->
                    val safeTitle = video.title
                        .replace("\t", " ")
                        .replace("\n", " ")
                        .replace("\r", " ")
                    "${video.id}\t${safeTitle}\t${video.durationSeconds}"
                }
                val payload = if (rows.isEmpty()) {
                    "$QUEUE_CACHE_TIMESTAMP_PREFIX${System.currentTimeMillis()}"
                } else {
                    "$QUEUE_CACHE_TIMESTAMP_PREFIX${System.currentTimeMillis()}\n$rows"
                }
                file.writeText(payload)
            }.onFailure {
                AppLog.e("PlaylistHistoryStore: failed to save mini queue cache", it)
            }
        }
    }

    fun loadLastMiniQueue(
        limit: Int = 40,
        maxAgeMs: Long? = null,
        nowMs: Long = System.currentTimeMillis()
    ): List<PlaylistVideo> {
        synchronized(storageLock) {
            val file = queueCacheFile ?: return emptyList()
            if (!file.exists()) return emptyList()

            return runCatching {
                val lines = file.readLines()
                if (lines.isEmpty()) return@runCatching emptyList()

                val firstLine = lines.first()
                val hasTimestampHeader = firstLine.startsWith(QUEUE_CACHE_TIMESTAMP_PREFIX)
                val savedAtMs = if (hasTimestampHeader) {
                    firstLine.removePrefix(QUEUE_CACHE_TIMESTAMP_PREFIX).toLongOrNull() ?: file.lastModified()
                } else {
                    file.lastModified()
                }

                if (maxAgeMs != null && maxAgeMs > 0L) {
                    val ageMs = nowMs - savedAtMs
                    if (ageMs > maxAgeMs) {
                        AppLog.d("PlaylistHistoryStore: cached mini queue is expired (ageMs=$ageMs > maxAgeMs=$maxAgeMs)")
                        return@runCatching emptyList()
                    }
                }

                val entryLines = if (hasTimestampHeader) lines.drop(1) else lines
                val items = entryLines.mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size != 3) return@mapNotNull null
                    val videoId = parts[0]
                    val title = parts[1]
                    val durationSeconds = parts[2].toIntOrNull() ?: 0

                    PlaylistVideo(
                        id = videoId,
                        title = title,
                        durationSeconds = durationSeconds,
                        status = WatchStatus.NOT_STARTED,
                        resumeAtSeconds = 0
                    )
                }.take(limit)
                AppLog.d("PlaylistHistoryStore: loaded ${items.size} items from cached mini queue")
                items
            }.getOrElse {
                AppLog.e("PlaylistHistoryStore: error loading last mini queue cache", it)
                emptyList()
            }
        }
    }

    private fun persistIfConfigured() {
        synchronized(storageLock) {
            if (loadedFromDisk) {
                persistToDiskLocked()
            }
        }
    }
}
