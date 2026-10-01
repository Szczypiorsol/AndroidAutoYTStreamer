package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class PlaybackQueueBridgeTest {
    @Test
    fun `bridge stores queue snapshot copy`() {
        val queue = mutableListOf(
            PlaylistVideo("1", "Video 1", status = WatchStatus.NOT_STARTED)
        )

        PlaybackQueueBridge.update(
            queue = queue,
            playback = PlaybackSnapshot(currentVideo = queue.first(), isPlaying = false, positionSeconds = 12)
        )

        queue += PlaylistVideo("2", "Video 2", status = WatchStatus.NOT_STARTED)
        val snapshot = PlaybackQueueBridge.snapshot()

        assertEquals(1, snapshot.queue.size)
        assertEquals("1", snapshot.queue.first().id)
        assertEquals(12, snapshot.playback.positionSeconds)
        assertNotSame(queue, snapshot.queue)
    }
}

