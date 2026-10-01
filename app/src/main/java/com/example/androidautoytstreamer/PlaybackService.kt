package com.example.androidautoytstreamer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer? = player
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun playVideo(videoId: String) {
        val mediaItem = MediaItem.fromUri("https://www.youtube.com/watch?v=$videoId")
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }
}

