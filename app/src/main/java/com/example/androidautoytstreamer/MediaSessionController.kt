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
        AppLog.d("MediaSessionController.play requested")
        controllerFuture.addListener({
            runCatching {
                val controller = controllerFuture.get()
                controller.play()
                AppLog.d("MediaSessionController.play dispatched to controller")
            }.onFailure { AppLog.e("MediaSessionController.play failed", it) }
        }, { it.run() })
    }

    fun pause() {
        AppLog.d("MediaSessionController.pause requested")
        controllerFuture.addListener({
            runCatching {
                val controller = controllerFuture.get()
                controller.pause()
                AppLog.d("MediaSessionController.pause dispatched to controller")
            }.onFailure { AppLog.e("MediaSessionController.pause failed", it) }
        }, { it.run() })
    }

    fun next() {
        AppLog.d("MediaSessionController.next requested")
        controllerFuture.addListener({
            runCatching {
                val controller = controllerFuture.get()
                controller.seekToNextMediaItem()
                AppLog.d("MediaSessionController.next dispatched to controller")
            }.onFailure { AppLog.e("MediaSessionController.next failed", it) }
        }, { it.run() })
    }

    fun previous() {
        AppLog.d("MediaSessionController.previous requested")
        controllerFuture.addListener({
            runCatching {
                val controller = controllerFuture.get()
                controller.seekToPreviousMediaItem()
                AppLog.d("MediaSessionController.previous dispatched to controller")
            }.onFailure { AppLog.e("MediaSessionController.previous failed", it) }
        }, { it.run() })
    }
}
