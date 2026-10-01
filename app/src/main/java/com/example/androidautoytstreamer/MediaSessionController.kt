package com.example.androidautoytstreamer

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MediaSessionController(
    context: Context
) {
    private val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    private val controllerFuture: ListenableFuture<MediaController> = MediaController.Builder(context, sessionToken).buildAsync()

    fun play() {
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            controller.play()
        }, { it.run() })
    }

    fun pause() {
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            controller.pause()
        }, { it.run() })
    }

    fun next() {
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            controller.seekToNextMediaItem()
        }, { it.run() })
    }

    fun previous() {
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            controller.seekToPreviousMediaItem()
        }, { it.run() })
    }
}

