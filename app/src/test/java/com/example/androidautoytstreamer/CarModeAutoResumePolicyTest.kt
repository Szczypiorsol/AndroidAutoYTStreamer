package com.example.androidautoytstreamer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarModeAutoResumePolicyTest {
    @Test
    fun `auto resume triggers when entering car mode with paused active item`() {
        val shouldResume = shouldAutoResumeOnCarModeTransition(
            wasInCarMode = false,
            isInCarMode = true,
            snapshot = PlaybackSnapshot(
                currentVideo = PlaylistVideo("1", "Video 1", status = WatchStatus.IN_PROGRESS),
                isPlaying = false,
                queueEnded = false
            )
        )

        assertTrue(shouldResume)
    }

    @Test
    fun `auto resume does not trigger when already in car mode`() {
        val shouldResume = shouldAutoResumeOnCarModeTransition(
            wasInCarMode = true,
            isInCarMode = true,
            snapshot = PlaybackSnapshot(
                currentVideo = PlaylistVideo("1", "Video 1"),
                isPlaying = false,
                queueEnded = false
            )
        )

        assertFalse(shouldResume)
    }

    @Test
    fun `auto resume does not trigger when queue ended`() {
        val shouldResume = shouldAutoResumeOnCarModeTransition(
            wasInCarMode = false,
            isInCarMode = true,
            snapshot = PlaybackSnapshot(
                currentVideo = PlaylistVideo("1", "Video 1"),
                isPlaying = false,
                queueEnded = true
            )
        )

        assertFalse(shouldResume)
    }

    @Test
    fun `auto resume does not trigger without current video`() {
        val shouldResume = shouldAutoResumeOnCarModeTransition(
            wasInCarMode = false,
            isInCarMode = true,
            snapshot = PlaybackSnapshot(currentVideo = null, isPlaying = false, queueEnded = false)
        )

        assertFalse(shouldResume)
    }
}

