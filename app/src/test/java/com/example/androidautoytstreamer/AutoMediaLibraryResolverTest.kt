package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AutoMediaLibraryResolverTest {
    @Test
    fun `resolve resume uses current video`() {
        val queue = listOf(
            PlaylistVideo("a", "Video A", status = WatchStatus.NOT_STARTED),
            PlaylistVideo("b", "Video B", status = WatchStatus.NOT_STARTED)
        )
        val playback = PlaybackSnapshot(currentVideo = queue[1], isPlaying = false, positionSeconds = 22)

        val video = selectVideoForAutoMediaId("queue_resume", queue, playback)

        assertNotNull(video)
        assertEquals("b", video?.id)
    }

    @Test
    fun `resolve queue item uses media id mapping`() {
        val queue = listOf(
            PlaylistVideo("abc", "Video ABC", status = WatchStatus.NOT_STARTED)
        )

        val video = selectVideoForAutoMediaId("queue_abc", queue, PlaybackSnapshot())

        assertNotNull(video)
        assertEquals("abc", video?.id)
    }

    @Test
    fun `resolve returns null for unknown media id`() {
        val video = selectVideoForAutoMediaId("unknown", emptyList(), PlaybackSnapshot())
        assertNull(video)
    }

    @Test
    fun `resume falls back to first non completed queue item`() {
        val queue = listOf(
            PlaylistVideo("done", "Done", status = WatchStatus.COMPLETED),
            PlaylistVideo("next", "Next", status = WatchStatus.NOT_STARTED)
        )

        val video = selectVideoForAutoMediaId("queue_resume", queue, PlaybackSnapshot())

        assertNotNull(video)
        assertEquals("next", video?.id)
    }

    @Test
    fun `queue media id does not resolve item outside queue`() {
        val queue = listOf(PlaylistVideo("abc", "Video ABC", status = WatchStatus.NOT_STARTED))

        val video = selectVideoForAutoMediaId("queue_missing", queue, PlaybackSnapshot())

        assertNull(video)
    }
}

