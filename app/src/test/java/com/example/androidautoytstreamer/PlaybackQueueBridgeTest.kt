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

    @Test
    fun `listener receives updates and can be removed`() {
        var callbackCount = 0
        var lastQueueSize = -1
        val listener = PlaybackQueueListener { snapshot ->
            callbackCount += 1
            lastQueueSize = snapshot.queue.size
        }

        PlaybackQueueBridge.addListener(listener)
        PlaybackQueueBridge.update(
            queue = listOf(PlaylistVideo("x", "Video X")),
            playback = PlaybackSnapshot()
        )

        val countAfterUpdate = callbackCount
        PlaybackQueueBridge.removeListener(listener)
        PlaybackQueueBridge.update(
            queue = listOf(PlaylistVideo("x", "Video X"), PlaylistVideo("y", "Video Y")),
            playback = PlaybackSnapshot()
        )

        assertEquals(1, lastQueueSize)
        assertEquals(countAfterUpdate, callbackCount)
    }
}

