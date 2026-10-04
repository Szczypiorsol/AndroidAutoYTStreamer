package com.example.androidautoytstreamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackTransitionResolverTest {
    @Test
    fun `returns target when selected item is playable`() {
        val queue = listOf(
            PlaylistVideo("a", "A", status = WatchStatus.NOT_STARTED),
            PlaylistVideo("b", "B", status = WatchStatus.NOT_STARTED)
        )

        val resolved = resolvePlayableIndexForNavigation(queue, targetIndex = 1, previousIndex = 0)

        assertEquals(1, resolved)
    }

    @Test
    fun `forward navigation skips completed to next playable`() {
        val queue = listOf(
            PlaylistVideo("a", "A", status = WatchStatus.IN_PROGRESS),
            PlaylistVideo("b", "B", status = WatchStatus.COMPLETED),
            PlaylistVideo("c", "C", status = WatchStatus.NOT_STARTED)
        )

        val resolved = resolvePlayableIndexForNavigation(queue, targetIndex = 1, previousIndex = 0)

        assertEquals(2, resolved)
    }

    @Test
    fun `backward navigation skips completed to previous playable`() {
        val queue = listOf(
            PlaylistVideo("a", "A", status = WatchStatus.NOT_STARTED),
            PlaylistVideo("b", "B", status = WatchStatus.COMPLETED),
            PlaylistVideo("c", "C", status = WatchStatus.IN_PROGRESS)
        )

        val resolved = resolvePlayableIndexForNavigation(queue, targetIndex = 1, previousIndex = 2)

        assertEquals(0, resolved)
    }

    @Test
    fun `returns null when queue has only completed items`() {
        val queue = listOf(
            PlaylistVideo("a", "A", status = WatchStatus.COMPLETED),
            PlaylistVideo("b", "B", status = WatchStatus.COMPLETED)
        )

        val resolved = resolvePlayableIndexForNavigation(queue, targetIndex = 0, previousIndex = 1)

        assertNull(resolved)
    }
}

