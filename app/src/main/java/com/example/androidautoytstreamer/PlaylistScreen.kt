package com.example.androidautoytstreamer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistScreen(modifier: Modifier = Modifier) {
    val viewModel = remember { PlaylistViewModel() }
    var playlistInput by remember { mutableStateOf("PL8A5A9D5E0AF1D4F4") }
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
            text = "Playlist queue",
            style = MaterialTheme.typography.headlineMedium
        )

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                viewModel.nextVideo()
                loadedQueue = viewModel.snapshot()
            }) {
                Text("Next")
            }
            Button(onClick = {
                viewModel.previousVideo()
                loadedQueue = viewModel.snapshot()
            }) {
                Text("Previous")
            }
            Button(onClick = {
                val currentVideo = viewModel.currentVideo()
                if (currentVideo != null) {
                    viewModel.markCompleted(currentVideo.id)
                    loadedQueue = viewModel.snapshot()
                }
            }) {
                Text("Done")
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
