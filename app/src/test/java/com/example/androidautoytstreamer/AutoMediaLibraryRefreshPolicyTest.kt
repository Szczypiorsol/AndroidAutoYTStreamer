package com.example.androidautoytstreamer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMediaLibraryRefreshPolicyTest {
    @Test
    fun `notifies when queue version changes`() {
        val snapshot = PlaybackQueueSnapshot(
            queue = listOf(PlaylistVideo("1", "One")),
            playback = PlaybackSnapshot(),
            queueVersion = 2L
        )

        val shouldNotify = shouldNotifyBrowseRefresh(
            snapshot = snapshot,
            lastQueueVersion = 1L,
            lastPlayback = PlaybackSnapshot()
        )

        assertTrue(shouldNotify)
    }

    @Test
    fun `notifies when current video changes without queue mutation`() {
        val sameQueueVersion = 5L
        val snapshot = PlaybackQueueSnapshot(
            queue = listOf(PlaylistVideo("1", "One"), PlaylistVideo("2", "Two")),
            playback = PlaybackSnapshot(currentVideo = PlaylistVideo("2", "Two"), isPlaying = true),
            queueVersion = sameQueueVersion
        )

        val shouldNotify = shouldNotifyBrowseRefresh(
            snapshot = snapshot,
            lastQueueVersion = sameQueueVersion,
            lastPlayback = PlaybackSnapshot(currentVideo = PlaylistVideo("1", "One"), isPlaying = true)
        )

        assertTrue(shouldNotify)
    }

    @Test
    fun `notifies when play state toggles`() {
        val sameQueueVersion = 8L
        val snapshot = PlaybackQueueSnapshot(
            queue = listOf(PlaylistVideo("1", "One")),
            playback = PlaybackSnapshot(currentVideo = PlaylistVideo("1", "One"), isPlaying = false),
            queueVersion = sameQueueVersion
        )

        val shouldNotify = shouldNotifyBrowseRefresh(
            snapshot = snapshot,
            lastQueueVersion = sameQueueVersion,
            lastPlayback = PlaybackSnapshot(currentVideo = PlaylistVideo("1", "One"), isPlaying = true)
        )

        assertTrue(shouldNotify)
    }

    @Test
    fun `does not notify when only playback position changes`() {
        val sameQueueVersion = 11L
        val snapshot = PlaybackQueueSnapshot(
            queue = listOf(PlaylistVideo("1", "One")),
            playback = PlaybackSnapshot(
                currentVideo = PlaylistVideo("1", "One"),
                isPlaying = true,
                positionSeconds = 120,
                queueEnded = false
            ),
            queueVersion = sameQueueVersion
        )

        val shouldNotify = shouldNotifyBrowseRefresh(
            snapshot = snapshot,
            lastQueueVersion = sameQueueVersion,
            lastPlayback = PlaybackSnapshot(
                currentVideo = PlaylistVideo("1", "One"),
                isPlaying = true,
                positionSeconds = 15,
                queueEnded = false
            )
        )

        assertFalse(shouldNotify)
    }
}

