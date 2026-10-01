package com.example.androidautoytstreamer

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistHistoryStoreTest {
    @Test
    fun `history persists to disk and reloads`() {
        val tempDir = Files.createTempDirectory("history-store-test").toFile()
        PlaylistHistoryStore.clearInMemoryForTests()
        PlaylistHistoryStore.initialize(tempDir)

        val store = PlaylistHistoryStore()
        store.markPaused("video-1", 37)
        store.markCompleted("video-2")

        PlaylistHistoryStore.clearInMemoryForTests()
        PlaylistHistoryStore.initialize(tempDir)

        val reloaded = PlaylistHistoryStore().applyToQueue(
            listOf(
                PlaylistVideo("video-1", "Video 1"),
                PlaylistVideo("video-2", "Video 2")
            )
        )

        assertEquals(WatchStatus.IN_PROGRESS, reloaded[0].status)
        assertEquals(37, reloaded[0].resumeAtSeconds)
        assertTrue(reloaded[1].status == WatchStatus.COMPLETED)
    }

    @Test
    fun `mini queue cache reloads with history state`() {
        val tempDir = Files.createTempDirectory("history-queue-cache-test").toFile()
        PlaylistHistoryStore.clearInMemoryForTests()
        PlaylistHistoryStore.initialize(tempDir)

        val store = PlaylistHistoryStore()
        val queue = listOf(
            PlaylistVideo("video-1", "Video 1", durationSeconds = 100),
            PlaylistVideo("video-2", "Video 2", durationSeconds = 200)
        )
        store.saveLastMiniQueue(queue)
        store.markPaused("video-1", 25)
        store.markCompleted("video-2")

        PlaylistHistoryStore.clearInMemoryForTests()
        PlaylistHistoryStore.initialize(tempDir)

        val reloadedQueue = PlaylistHistoryStore().loadLastMiniQueue()
        val withHistory = PlaylistHistoryStore().applyToQueue(reloadedQueue)

        assertEquals(2, withHistory.size)
        assertEquals(WatchStatus.IN_PROGRESS, withHistory[0].status)
        assertEquals(25, withHistory[0].resumeAtSeconds)
        assertEquals(WatchStatus.COMPLETED, withHistory[1].status)
    }

    @Test
    fun `mini queue cache expires when older than max age`() {
        val tempDir = Files.createTempDirectory("history-queue-cache-expire-test").toFile()
        PlaylistHistoryStore.clearInMemoryForTests()
        PlaylistHistoryStore.initialize(tempDir)

        val store = PlaylistHistoryStore()
        store.saveLastMiniQueue(
            listOf(PlaylistVideo("video-1", "Video 1", durationSeconds = 100))
        )

        val loaded = store.loadLastMiniQueue(
            maxAgeMs = 1_000L,
            nowMs = System.currentTimeMillis() + 10_000L
        )

        assertTrue(loaded.isEmpty())
    }
}

