package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlaylistQueueManagerTest {
    @Test
    fun `queue skips completed videos and resumes unfinished item`() {
        val queueManager = PlaylistQueueManager()
        val videos = listOf(
            PlaylistVideo("1", "Video 1", status = WatchStatus.COMPLETED),
            PlaylistVideo("2", "Video 2", status = WatchStatus.IN_PROGRESS, resumeAtSeconds = 30),
            PlaylistVideo("3", "Video 3", status = WatchStatus.NOT_STARTED),
            PlaylistVideo("4", "Video 4", status = WatchStatus.COMPLETED)
        )

        queueManager.setQueue(videos)

        val current = queueManager.current()
        assertNotNull(current)
        assertEquals("2", current!!.id)
        assertEquals(2, queueManager.remainingVideos().size)

        val next = queueManager.next()
        assertNotNull(next)
        assertEquals("3", next!!.id)

        queueManager.markCompleted("3")
        assertEquals("4", queueManager.current()?.id)
    }
}
