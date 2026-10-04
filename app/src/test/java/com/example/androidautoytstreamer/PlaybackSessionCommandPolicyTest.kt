package com.example.androidautoytstreamer

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionCommandPolicyTest {
    @Test
    fun `play pause command is blocked when queue already ended`() {
        assertFalse(isPlayerCommandAllowed(queueEnded = true, playerCommand = Player.COMMAND_PLAY_PAUSE))
    }

    @Test
    fun `non play commands remain allowed when queue ended`() {
        assertTrue(isPlayerCommandAllowed(queueEnded = true, playerCommand = Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(isPlayerCommandAllowed(queueEnded = true, playerCommand = Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
    }

    @Test
    fun `all commands are allowed while queue active`() {
        assertTrue(isPlayerCommandAllowed(queueEnded = false, playerCommand = Player.COMMAND_PLAY_PAUSE))
        assertTrue(isPlayerCommandAllowed(queueEnded = false, playerCommand = Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
    }
}

