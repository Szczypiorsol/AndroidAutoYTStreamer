package com.example.androidautoytstreamer

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PlaylistLoadRetryAction {
    LOAD_PLAYLISTS,
    LOAD_SELECTED_PLAYLIST
}

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    signedInEmail: String? = null,
    logFilePath: String? = null,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onOpenCurrentInYoutube: (String) -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onQueueLoaded: (List<PlaylistVideo>) -> Unit = {},
    playbackSnapshot: PlaybackSnapshot = PlaybackSnapshot(),
    viewModel: PlaylistViewModel? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember(activity) { if (activity != null) GoogleAuthManager(activity) else null }
    val playlistRepository = remember(authManager) { if (authManager != null) YouTubePrivatePlaylistRepository(context, authManager) else null }
    val resolvedViewModel = viewModel ?: remember { PlaylistViewModel() }

    var playlistInput by remember { mutableStateOf("") }
    var privatePlaylists by remember { mutableStateOf<List<PlaylistSummary>>(emptyList()) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var playlistFilter by remember { mutableStateOf("") }
    var playlistError by remember { mutableStateOf<String?>(null) }
    var isLoadingPlaylists by remember { mutableStateOf(false) }
    var isLoadingPlaylistItems by remember { mutableStateOf(false) }
    var retryAction by remember { mutableStateOf<PlaylistLoadRetryAction?>(null) }
    var loadedQueue by remember { mutableStateOf(resolvedViewModel.snapshot()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loadedQueue) {
        resolvedViewModel.loadPlaylist(loadedQueue)
    }

    fun loadPrivatePlaylists() {
        if (playlistRepository == null) {
            playlistError = "Zaloguj się przez Google, aby wczytać prywatne playlisty"
            retryAction = null
            AppLog.e("PlaylistScreen: Attempted to load private playlists without Google auth")
            return
        }

        scope.launch {
            AppLog.d("PlaylistScreen: Loading private playlists...")
            isLoadingPlaylists = true
            playlistError = null
            retryAction = null

            val result = withContext(Dispatchers.IO) {
                playlistRepository.loadPlaylists()
            }

            privatePlaylists = result.getOrElse { error ->
                AppLog.e("PlaylistScreen: Loading private playlists failed: ${error.message}", error)
                playlistError = error.message ?: "Unable to load playlists"
                retryAction = PlaylistLoadRetryAction.LOAD_PLAYLISTS
                emptyList()
            }
            isLoadingPlaylists = false
        }
    }

    fun loadPlaylistItems(playlistId: String) {
        if (playlistRepository == null) {
            playlistError = "Zaloguj się przez Google, aby pobrać playlistę"
            retryAction = null
            AppLog.e("PlaylistScreen: Attempted to load playlist $playlistId without Google auth")
            return
        }

        scope.launch {
            AppLog.d("PlaylistScreen: Loading playlist items for playlistId=$playlistId...")
            selectedPlaylistId = playlistId
            isLoadingPlaylistItems = true
            playlistError = null
            retryAction = null

            val items = withContext(Dispatchers.IO) {
                playlistRepository.loadPlaylistItems(playlistId)
            }.getOrElse { error ->
                AppLog.e("PlaylistScreen: Loading items failed for playlistId=$playlistId: ${error.message}", error)
                playlistError = error.message ?: "Unable to load playlist items"
                retryAction = PlaylistLoadRetryAction.LOAD_SELECTED_PLAYLIST
                emptyList()
            }

            if (items.isNotEmpty()) {
                AppLog.d("PlaylistScreen: Loaded ${items.size} items for playlistId=$playlistId")
                loadedQueue = items
                onQueueLoaded(items)
                retryAction = null
            } else if (playlistError == null) {
                AppLog.d("PlaylistScreen: Playlist $playlistId is empty")
                playlistError = "Playlist is empty"
                retryAction = null
            }

            isLoadingPlaylistItems = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Car mode playlist",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (isSignedIn) "Google: signed in" else "Google: not signed in",
            style = MaterialTheme.typography.titleMedium
        )

        if (signedInEmail != null) {
            Text(
                text = "Account: $signedInEmail",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (logFilePath != null) {
            Text(
                text = "App log: $logFilePath",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) {
                Text(if (isSignedIn) "Sign in again" else "Google sign in")
            }
            Button(
                onClick = {
                    onGoogleSignOut()
                    privatePlaylists = emptyList()
                    playlistFilter = ""
                    selectedPlaylistId = null
                    retryAction = null
                    playlistError = null
                },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) {
                Text("Sign out")
            }
        }

        if (isSignedIn && playlistRepository != null) {
            Button(
                onClick = { loadPrivatePlaylists() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load my playlists")
            }
        }

        if (isLoadingPlaylists || isLoadingPlaylistItems) {
            CircularProgressIndicator()
        }

        if (playlistError != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = playlistError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                when (retryAction) {
                    PlaylistLoadRetryAction.LOAD_PLAYLISTS -> {
                        Button(onClick = { loadPrivatePlaylists() }) {
                            Text("Retry")
                        }
                    }

                    PlaylistLoadRetryAction.LOAD_SELECTED_PLAYLIST -> {
                        val playlistId = selectedPlaylistId
                        if (playlistId != null) {
                            Button(onClick = { loadPlaylistItems(playlistId) }) {
                                Text("Retry playlist")
                            }
                        }
                    }

                    null -> Unit
                }
            }
        }

        if (privatePlaylists.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("My playlists")
                    OutlinedTextField(
                        value = playlistFilter,
                        onValueChange = { playlistFilter = it },
                        label = { Text("Filter playlists by name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val filteredPlaylists = privatePlaylists.filter { playlist ->
                        val query = playlistFilter.trim()
                        query.isEmpty() ||
                            playlist.title.contains(query, ignoreCase = true) ||
                            playlist.description.contains(query, ignoreCase = true)
                    }
                    Text(
                        text = "Found: ${filteredPlaylists.size}/${privatePlaylists.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredPlaylists.forEach { playlist ->
                            Button(
                                onClick = { loadPlaylistItems(playlist.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(playlist.title)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Queue items: ${resolvedViewModel.remainingVideos().size}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = when {
                playbackSnapshot.queueEnded -> "Playback: queue finished"
                playbackSnapshot.isPlaying -> "Playback: playing"
                else -> "Playback: paused"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onPlay()
                    val currentVideo = resolvedViewModel.currentVideo()
                    if (currentVideo != null) {
                        resolvedViewModel.markStarted(currentVideo.id)
                        loadedQueue = resolvedViewModel.snapshot()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Play")
            }
            Button(
                onClick = {
                    onPause()
                    val currentVideo = resolvedViewModel.currentVideo()
                    if (currentVideo != null) {
                        resolvedViewModel.markPaused(currentVideo.id, playbackSnapshot.positionSeconds)
                        loadedQueue = resolvedViewModel.snapshot()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
            ) {
                Text("Pause")
            }
        }

        val currentVideoIdForFallback = (playbackSnapshot.currentVideo ?: resolvedViewModel.currentVideo())?.id
        if (currentVideoIdForFallback != null) {
            Button(
                onClick = { onOpenCurrentInYoutube(currentVideoIdForFallback) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open current in YouTube")
            }
        }

        Text(
            text = "Note: if in-app playback does not start on phone, use 'Open current in YouTube'.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onPrevious()
                    resolvedViewModel.previousVideo()
                    loadedQueue = resolvedViewModel.snapshot()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
            ) {
                Text("Prev")
            }
            Button(
                onClick = {
                    onNext()
                    resolvedViewModel.nextVideo()
                    loadedQueue = resolvedViewModel.snapshot()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
            ) {
                Text("Next")
            }
        }

        OutlinedTextField(
            value = playlistInput,
            onValueChange = { playlistInput = it },
            label = { Text("YouTube playlist URL or ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    AppLog.d("PlaylistScreen: User requested loading playlist from input '$playlistInput'")
                    playlistError = null
                    retryAction = null
                    val result = withContext(Dispatchers.IO) {
                        resolvedViewModel.loadFromPlaylistInput(playlistInput)
                    }
                    result.getOrNull()?.let {
                        AppLog.d("PlaylistScreen: Loaded playlist with ${it.size} items")
                        loadedQueue = it
                        onQueueLoaded(it)
                    } ?: run {
                        val errMsg = result.exceptionOrNull()?.message ?: "Unable to load playlist"
                        AppLog.e("PlaylistScreen: Failed to load playlist from input: $errMsg")
                        playlistError = errMsg
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load playlist")
        }

        val current = playbackSnapshot.currentVideo ?: resolvedViewModel.currentVideo()
        if (current != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Current")
                    Text(text = current.title)
                    Text(text = "Status: ${current.status.name}")
                    Text(text = "Resume: ${playbackSnapshot.positionSeconds}s")
                }
            }
        } else {
            Text(
                text = "No active video. Load a playlist above to start the phone flow.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val remainingVideos = resolvedViewModel.remainingVideos()
            if (remainingVideos.isEmpty()) {
                item {
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(remainingVideos) { video ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(video.title)
                        Text("${video.status.name} • ${video.resumeAtSeconds}s")
                    }
                }
            }
        }
    }
}
