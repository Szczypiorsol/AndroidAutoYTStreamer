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
}

