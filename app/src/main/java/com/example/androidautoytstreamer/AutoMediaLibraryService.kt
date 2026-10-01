package com.example.androidautoytstreamer

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession.ControllerInfo
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class AutoMediaLibraryService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Playlists")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
            }

            val queueSnapshot = PlaybackQueueBridge.snapshot()
            val queue = queueSnapshot.queue
            val playback = queueSnapshot.playback

            if (queue.isEmpty()) {
                val emptyItem = MediaItem.Builder()
                    .setMediaId(EMPTY_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Queue is empty")
                            .setSubtitle("Load playlist in phone app")
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(emptyItem), params))
            }

            val items = mutableListOf<MediaItem>()

            playback.currentVideo?.let { current ->
                items += MediaItem.Builder()
                    .setMediaId(RESUME_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Resume: ${current.title}")
                            .setSubtitle("${playback.positionSeconds}s")
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }

            queue.take(MAX_LIBRARY_ITEMS).forEach { video ->
                val isCurrent = playback.currentVideo?.id == video.id
                val statusText = when {
                    video.status == WatchStatus.COMPLETED -> "completed"
                    isCurrent && playback.isPlaying -> "playing"
                    isCurrent -> "paused"
                    else -> "queued"
                }

                items += MediaItem.Builder()
                    .setMediaId("queue_${video.id}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(video.title)
                            .setSubtitle(statusText)
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }

            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val result = if (parentId == ROOT_ID) {
                LibraryResult.ofVoid()
            } else {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            }
            return Futures.immediateFuture(result)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaLibrarySession = MediaLibrarySession.Builder(this, player!!, libraryCallback)
            .setId("auto-media-library")
            .build()
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        player?.release()
        mediaLibrarySession = null
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "root"
        private const val EMPTY_ID = "queue_empty"
        private const val RESUME_ID = "queue_resume"
        private const val MAX_LIBRARY_ITEMS = 40
    }
}

