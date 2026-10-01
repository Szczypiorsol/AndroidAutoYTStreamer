package com.example.androidautoytstreamer

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember(activity) { if (activity != null) GoogleAuthManager(activity) else null }
    val playlistRepository = remember(authManager) { if (authManager != null) YouTubePrivatePlaylistRepository(context, authManager) else null }

    val viewModel = remember { PlaylistViewModel() }
    var playlistInput by remember { mutableStateOf("PL8A5A9D5E0AF1D4F4") }
    var isPlaying by remember { mutableStateOf(false) }
    var privatePlaylists by remember { mutableStateOf<List<PlaylistSummary>>(emptyList()) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var loadedQueue by remember {
        mutableStateOf(
            listOf(
                PlaylistVideo("1", "Film 1", 240, WatchStatus.NOT_STARTED),
                PlaylistVideo("2", "Film 2", 300, WatchStatus.NOT_STARTED),
                PlaylistVideo("3", "Film 3", 180, WatchStatus.COMPLETED),
                PlaylistVideo("4", "Film 4", 420, WatchStatus.IN_PROGRESS, 90)
            )
        )
    }

    viewModel.loadPlaylist(loadedQueue)

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(if (isSignedIn) "Sign in again" else "Google sign in")
            }
            Button(
                onClick = {
                    onGoogleSignOut()
                    privatePlaylists = emptyList()
                    selectedPlaylistId = null
                },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Sign out")
            }
        }

        if (isSignedIn && playlistRepository != null) {
            Button(
                onClick = {
                    privatePlaylists = playlistRepository.loadPlaylists().getOrElse { emptyList() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load my playlists")
            }
        }

        if (privatePlaylists.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("My playlists")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        privatePlaylists.forEach { playlist ->
                            Button(
                                onClick = {
                                    selectedPlaylistId = playlist.id
                                    val items = playlistRepository?.loadPlaylistItems(playlist.id)?.getOrElse { emptyList() } ?: emptyList()
                                    if (items.isNotEmpty()) {
                                        loadedQueue = items
                                        viewModel.loadPlaylist(items)
                                    }
                                },
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
            text = if (isPlaying) "Playback: playing" else "Playback: paused",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onPlay()
                    isPlaying = true
                    val currentVideo = viewModel.currentVideo()
                    if (currentVideo != null) {
                        viewModel.markStarted(currentVideo.id)
                        loadedQueue = viewModel.snapshot()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Play")
            }
            Button(
                onClick = {
                    onPause()
                    isPlaying = false
                    val currentVideo = viewModel.currentVideo()
                    if (currentVideo != null) {
                        viewModel.markPaused(currentVideo.id, currentVideo.resumeAtSeconds)
                        loadedQueue = viewModel.snapshot()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Text("Pause")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onPrevious()
                    viewModel.previousVideo()
                    loadedQueue = viewModel.snapshot()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Text("Prev")
            }
            Button(
                onClick = {
                    onNext()
                    viewModel.nextVideo()
                    loadedQueue = viewModel.snapshot()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
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
                val result = viewModel.loadFromPlaylistInput(playlistInput)
                result.getOrNull()?.let { loadedQueue = it }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load playlist")
        }

        val current = viewModel.currentVideo()
        if (current != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Current")
                    Text(text = current.title)
                    Text(text = "Status: ${current.status.name}")
                    Text(text = "Resume: ${current.resumeAtSeconds}s")
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.remainingVideos()) { video ->
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
