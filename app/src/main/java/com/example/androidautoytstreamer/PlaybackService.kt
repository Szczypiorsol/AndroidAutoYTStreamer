package com.example.androidautoytstreamer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class PlaybackSnapshot(
    val currentVideo: PlaylistVideo? = null,
    val isPlaying: Boolean = false,
    val positionSeconds: Int = 0,
    val queueEnded: Boolean = false
)

fun interface PlaybackStateListener {
    fun onPlaybackStateChanged(snapshot: PlaybackSnapshot)
}

class PlaybackService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val queue = mutableListOf<PlaylistVideo>()
    private var currentQueueIndex = -1
    private var lastKnownQueueIndex = -1
    private var queueEnded = false
    private val playbackListeners = CopyOnWriteArraySet<PlaybackStateListener>()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val resolvingIds = ConcurrentHashMap.newKeySet<String>()

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            AppLog.e("PlaybackService ExoPlayer error [code=${error.errorCodeName}]: ${error.message}", error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "STATE_IDLE"
                Player.STATE_BUFFERING -> "STATE_BUFFERING"
                Player.STATE_READY -> "STATE_READY"
                Player.STATE_ENDED -> "STATE_ENDED"
                else -> "UNKNOWN($playbackState)"
            }
            AppLog.d("PlaybackService ExoPlayer state changed -> $stateName")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val localPlayer = player ?: return
            val newIndex = localPlayer.currentMediaItemIndex
            AppLog.d("PlaybackService media transition: item='${mediaItem?.mediaId}', reason=$reason, newIndex=$newIndex")

            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                markCompletedAt(lastKnownQueueIndex)
            }

            if (newIndex in queue.indices) {
                currentQueueIndex = newIndex
                val resolvedIndex = resolvePlayableIndexForNavigation(
                    queue = queue,
                    targetIndex = newIndex,
                    previousIndex = lastKnownQueueIndex
                )
                if (resolvedIndex == null) {
                    endQueuePlayback()
                } else {
                    if (resolvedIndex != newIndex) {
                        queueEnded = false
                        currentQueueIndex = resolvedIndex
                        localPlayer.seekToDefaultPosition(resolvedIndex)
                        localPlayer.play()
                    } else {
                        queueEnded = false
                        markCurrentStarted()
                    }
                    ensureMediaItemResolved(currentQueueIndex)
                }
            }

            lastKnownQueueIndex = currentQueueIndex
            notifyPlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            AppLog.d("PlaybackService isPlaying changed -> $isPlaying (current=${currentVideo()?.title})")
            if (isPlaying) {
                markCurrentStarted()
            } else {
                saveCurrentProgress()
            }
            notifyPlaybackState()
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            val allowed = isPlayerCommandAllowed(queueEnded, playerCommand)
            AppLog.d("PlaybackService mediaSession onPlayerCommandRequest cmd=$playerCommand allowed=$allowed")
            return if (allowed) {
                SessionResult.RESULT_SUCCESS
            } else {
                SessionResult.RESULT_ERROR_INVALID_STATE
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
        fun getPlayer(): ExoPlayer? = player
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.initialize(this)
        AppLog.d("PlaybackService.onCreate")
        PlaylistHistoryStore.initialize(filesDir)
        player = ExoPlayer.Builder(this).build()
        player?.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(mediaSessionCallback)
            .build()
    }

    override fun onDestroy() {
        AppLog.d("PlaybackService.onDestroy")
        serviceScope.cancel()
        saveCurrentProgress()
        player?.removeListener(playerListener)
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLog.d("PlaybackService.onTaskRemoved")
        saveCurrentProgress()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder {
        AppLog.d("PlaybackService.onBind intent=$intent")
        return binder
    }

    fun setQueue(videos: List<PlaylistVideo>) {
        queue.clear()
        queue.addAll(videos)
        queueEnded = false

        if (queue.isEmpty()) {
            AppLog.d("PlaybackService.setQueue called with EMPTY queue")
            player?.clearMediaItems()
            currentQueueIndex = -1
            notifyPlaybackState()
            return
        }

        val preferredIndex = queue.indexOfFirst { it.status == WatchStatus.IN_PROGRESS }
            .takeIf { it != -1 }
            ?: queue.indexOfFirst { it.status == WatchStatus.NOT_STARTED }
            ?: 0

        currentQueueIndex = preferredIndex
        lastKnownQueueIndex = preferredIndex
        AppLog.d("PlaybackService.setQueue: count=${queue.size}, preferredIndex=$preferredIndex (${queue[preferredIndex].title})")

        val initialMediaItems = queue.map { video ->
            MediaItem.Builder()
                .setMediaId(video.id)
                .setUri("https://www.youtube.com/watch?v=${video.id}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .build()
                )
                .setTag(video.id)
                .build()
        }

        player?.setMediaItems(initialMediaItems, false)
        player?.seekTo(preferredIndex, (queue[preferredIndex].resumeAtSeconds * 1000L).coerceAtLeast(0L))
        player?.playWhenReady = true
        markCurrentStarted()
        notifyPlaybackState()

        ensureMediaItemResolved(preferredIndex)
    }

    fun playCurrent() {
        AppLog.d("PlaybackService.playCurrent: index=$currentQueueIndex video=${currentVideo()?.title}")
        if (player?.mediaItemCount == 0) return
        if (queueEnded) return
        ensureMediaItemResolved(currentQueueIndex)
        player?.play()
    }

    fun pause() {
        AppLog.d("PlaybackService.pause: index=$currentQueueIndex video=${currentVideo()?.title}")
        saveCurrentProgress()
        player?.pause()
        notifyPlaybackState()
    }

    fun resume() {
        AppLog.d("PlaybackService.resume: index=$currentQueueIndex video=${currentVideo()?.title}")
        if (player?.mediaItemCount == 0) return
        if (queueEnded) return
        ensureMediaItemResolved(currentQueueIndex)
        player?.play()
        notifyPlaybackState()
    }

    fun next(): PlaylistVideo? {
        saveCurrentProgress()
        val nextIndex = findNextPlayableIndex(currentQueueIndex + 1)
            ?: run {
                AppLog.d("PlaybackService.next: no more playable items in queue")
                endQueuePlayback()
                return null
            }
        AppLog.d("PlaybackService.next: advancing from index $currentQueueIndex to $nextIndex (${queue[nextIndex].title})")
        queueEnded = false
        currentQueueIndex = nextIndex
        lastKnownQueueIndex = nextIndex
        player?.seekToDefaultPosition(nextIndex)
        markCurrentStarted()
        notifyPlaybackState()
        ensureMediaItemResolved(nextIndex)
        player?.play()
        return queue[nextIndex]
    }

    fun previous(): PlaylistVideo? {
        saveCurrentProgress()
        val previousIndex = findPreviousPlayableIndex(currentQueueIndex - 1)
            ?: run {
                AppLog.d("PlaybackService.previous: no previous playable items in queue")
                return null
            }
        AppLog.d("PlaybackService.previous: going back from index $currentQueueIndex to $previousIndex (${queue[previousIndex].title})")
        queueEnded = false
        currentQueueIndex = previousIndex
        lastKnownQueueIndex = previousIndex
        player?.seekToDefaultPosition(previousIndex)
        markCurrentStarted()
        notifyPlaybackState()
        ensureMediaItemResolved(previousIndex)
        player?.play()
        return queue[previousIndex]
    }

    fun currentVideo(): PlaylistVideo? {
        return if (currentQueueIndex in queue.indices) queue[currentQueueIndex] else null
    }

    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.add(listener)
        listener.onPlaybackStateChanged(currentPlaybackSnapshot())
    }

    fun removePlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.remove(listener)
    }

    fun currentPlaybackSnapshot(): PlaybackSnapshot {
        val localPlayer = player
        return PlaybackSnapshot(
            currentVideo = currentVideo(),
            isPlaying = localPlayer?.isPlaying == true,
            positionSeconds = ((localPlayer?.currentPosition ?: 0L) / 1000L).toInt().coerceAtLeast(0),
            queueEnded = queueEnded
        )
    }

    fun markCurrentStarted() {
        if (currentQueueIndex !in queue.indices) return
        val current = queue[currentQueueIndex]
        queue[currentQueueIndex] = current.copy(status = WatchStatus.IN_PROGRESS)
    }

    fun markCurrentCompleted() {
        markCompletedAt(currentQueueIndex)
    }

    private fun ensureMediaItemResolved(index: Int) {
        if (index !in queue.indices) return
        val video = queue[index]

        if (resolvingIds.contains(video.id)) return
        resolvingIds.add(video.id)

        serviceScope.launch {
            try {
                val result = YouTubeStreamResolver.resolveAudioStreamUrl(video.id)
                val streamUrl = result.getOrNull()
                val localPlayer = player ?: return@launch

                if (!streamUrl.isNullOrBlank() && index in queue.indices && index < localPlayer.mediaItemCount) {
                    val currentItem = localPlayer.getMediaItemAt(index)
                    val currentUri = currentItem.localConfiguration?.uri?.toString()

                    if (currentUri != streamUrl) {
                        AppLog.d("PlaybackService: Resolved direct stream URL for index $index (id=${video.id})")
                        val updatedMediaItem = MediaItem.Builder()
                            .setMediaId(video.id)
                            .setUri(streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(video.title)
                                    .build()
                            )
                            .setTag(video.id)
                            .build()

                        val isPlayingThisIndex = localPlayer.currentMediaItemIndex == index
                        val currentPos = localPlayer.currentPosition
                        localPlayer.replaceMediaItem(index, updatedMediaItem)

                        if (isPlayingThisIndex) {
                            localPlayer.seekTo(index, currentPos)
                            localPlayer.prepare()
                            localPlayer.play()
                        }
                    }
                }

                if (index + 1 in queue.indices) {
                    val nextVideo = queue[index + 1]
                    YouTubeStreamResolver.resolveAudioStreamUrl(nextVideo.id)
                }
            } finally {
                resolvingIds.remove(video.id)
            }
        }
    }

    private fun saveCurrentProgress() {
        val localPlayer = player ?: return
        if (currentQueueIndex !in queue.indices) return

        val current = queue[currentQueueIndex]
        if (current.status == WatchStatus.COMPLETED) return

        val resumeSeconds = (localPlayer.currentPosition / 1000L).toInt().coerceAtLeast(0)
        AppLog.d("PlaybackService saveCurrentProgress: video=${current.id}, resumeAtSeconds=$resumeSeconds")
        queue[currentQueueIndex] = current.copy(
            status = WatchStatus.IN_PROGRESS,
            resumeAtSeconds = resumeSeconds
        )
        notifyPlaybackState()
    }

    private fun markCompletedAt(index: Int) {
        if (index !in queue.indices) return
        val current = queue[index]
        AppLog.d("PlaybackService markCompletedAt: index=$index, video=${current.id}")
        queue[index] = current.copy(status = WatchStatus.COMPLETED, resumeAtSeconds = 0)
        notifyPlaybackState()
    }

    private fun notifyPlaybackState() {
        val snapshot = currentPlaybackSnapshot()
        PlaybackQueueBridge.update(queue, snapshot)
        PlaylistHistoryStore().saveLastMiniQueue(queue)
        playbackListeners.forEach { listener ->
            listener.onPlaybackStateChanged(snapshot)
        }
    }

    private fun endQueuePlayback() {
        AppLog.d("PlaybackService endQueuePlayback: queue has ended")
        queueEnded = true
        player?.pause()
        notifyPlaybackState()
    }

    private fun findNextPlayableIndex(fromIndex: Int): Int? {
        if (queue.isEmpty()) return null
        for (index in fromIndex until queue.size) {
            if (queue[index].status != WatchStatus.COMPLETED) return index
        }
        return null
    }

    private fun findPreviousPlayableIndex(fromIndex: Int): Int? {
        if (queue.isEmpty()) return null
        for (index in fromIndex downTo 0) {
            if (queue[index].status != WatchStatus.COMPLETED) return index
        }
        return null
    }
}

internal fun isPlayerCommandAllowed(queueEnded: Boolean, playerCommand: Int): Boolean {
    if (!queueEnded) return true
    return playerCommand != Player.COMMAND_PLAY_PAUSE
}

internal fun resolvePlayableIndexForNavigation(
    queue: List<PlaylistVideo>,
    targetIndex: Int,
    previousIndex: Int
): Int? {
    if (targetIndex !in queue.indices) return null
    if (queue[targetIndex].status != WatchStatus.COMPLETED) return targetIndex

    val movedBackward = previousIndex in queue.indices && targetIndex < previousIndex
    val preferred = if (movedBackward) {
        findPlayableIndexBackward(queue, targetIndex - 1)
            ?: findPlayableIndexForward(queue, targetIndex + 1)
    } else {
        findPlayableIndexForward(queue, targetIndex + 1)
            ?: findPlayableIndexBackward(queue, targetIndex - 1)
    }
    return preferred
}

private fun findPlayableIndexForward(queue: List<PlaylistVideo>, fromIndex: Int): Int? {
    if (queue.isEmpty()) return null
    for (index in fromIndex until queue.size) {
        if (index in queue.indices && queue[index].status != WatchStatus.COMPLETED) return index
    }
    return null
}

private fun findPlayableIndexBackward(queue: List<PlaylistVideo>, fromIndex: Int): Int? {
    if (queue.isEmpty()) return null
    for (index in fromIndex downTo 0) {
        if (index in queue.indices && queue[index].status != WatchStatus.COMPLETED) return index
    }
    return null
}
