package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaylistViewModelTest {
    @Before
    fun setUp() {
        PlaylistHistoryStore.clearInMemoryForTests()
    }

    @Test
    fun `blank playlist input fails without changing queue`() {
        val initialQueue = listOf(
            PlaylistVideo("seed", "Seed video", status = WatchStatus.IN_PROGRESS, resumeAtSeconds = 12)
        )
        val viewModel = PlaylistViewModel(
            queueManager = PlaylistQueueManager(),
            loadPlaylistFromInput = { Result.failure(IllegalArgumentException("blank")) }
        )

        viewModel.loadPlaylist(initialQueue)

        val result = viewModel.loadFromPlaylistInput("   ")

        assertTrue(result.isFailure)
        assertEquals("seed", viewModel.currentVideo()?.id)
        assertEquals(12, viewModel.currentVideo()?.resumeAtSeconds)
    }

    @Test
    fun `load from playlist input updates queue when loader succeeds`() {
        val expectedQueue = listOf(
            PlaylistVideo("1", "Video 1", status = WatchStatus.COMPLETED),
            PlaylistVideo("2", "Video 2", status = WatchStatus.IN_PROGRESS, resumeAtSeconds = 45),
            PlaylistVideo("3", "Video 3", status = WatchStatus.NOT_STARTED)
        )

        val viewModel = PlaylistViewModel(
            queueManager = PlaylistQueueManager(),
            loadPlaylistFromInput = { rawInput ->
                assertEquals("https://youtube.com/playlist?list=test", rawInput)
                Result.success(expectedQueue)
            }
        )

        val result = viewModel.loadFromPlaylistInput("https://youtube.com/playlist?list=test")

        assertTrue(result.isSuccess)
        assertEquals("2", viewModel.currentVideo()?.id)
        assertEquals(3, viewModel.snapshot().size)
        assertEquals(2, viewModel.remainingVideos().size)
    }

    @Test
    fun `load from playlist input keeps current queue when loader fails`() {
        val initialQueue = listOf(
            PlaylistVideo("seed", "Seed video", status = WatchStatus.IN_PROGRESS, resumeAtSeconds = 12)
        )
        val viewModel = PlaylistViewModel(
            queueManager = PlaylistQueueManager(),
            loadPlaylistFromInput = { Result.failure(IllegalStateException("boom")) }
        )

        viewModel.loadPlaylist(initialQueue)

        val result = viewModel.loadFromPlaylistInput("invalid")

        assertTrue(result.isFailure)
        assertEquals("seed", viewModel.currentVideo()?.id)
        assertEquals(12, viewModel.currentVideo()?.resumeAtSeconds)
    }
}
