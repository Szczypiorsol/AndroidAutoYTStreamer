package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class PlaybackQueueBridgeTest {
    @Before
    fun setUp() {
        PlaybackQueueBridge.resetForTests()
    }

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
        assertEquals(1L, snapshot.queueVersion)
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

    @Test
    fun `queue version increments only when queue changes`() {
        PlaybackQueueBridge.update(
            queue = listOf(PlaylistVideo("1", "Video 1")),
            playback = PlaybackSnapshot(positionSeconds = 1)
        )
        val firstVersion = PlaybackQueueBridge.snapshot().queueVersion

        PlaybackQueueBridge.update(
            queue = listOf(PlaylistVideo("1", "Video 1")),
            playback = PlaybackSnapshot(positionSeconds = 99)
        )
        val secondVersion = PlaybackQueueBridge.snapshot().queueVersion

        PlaybackQueueBridge.update(
            queue = listOf(PlaylistVideo("1", "Video 1"), PlaylistVideo("2", "Video 2")),
            playback = PlaybackSnapshot(positionSeconds = 100)
        )
        val thirdVersion = PlaybackQueueBridge.snapshot().queueVersion

        assertEquals(firstVersion, secondVersion)
        assertEquals(firstVersion + 1L, thirdVersion)
    }
}

