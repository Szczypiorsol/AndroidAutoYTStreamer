package com.example.androidautoytstreamer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidautoytstreamer.ui.theme.AndroidAutoYTStreamerTheme

class MainActivity : ComponentActivity() {
    private var playbackService: PlaybackService? = null
    private var mediaSessionController: MediaSessionController? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            mediaSessionController = MediaSessionController(this@MainActivity)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            playbackService = null
            mediaSessionController = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this, PlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            AndroidAutoYTStreamerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaylistScreen(
                        modifier = Modifier.padding(innerPadding),
                        onPlay = { playbackService?.resume() ?: mediaSessionController?.play() },
                        onPause = { playbackService?.pause() ?: mediaSessionController?.pause() },
                        onNext = { playbackService?.playVideo("next") ?: mediaSessionController?.next() },
                        onPrevious = { playbackService?.playVideo("previous") ?: mediaSessionController?.previous() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    AndroidAutoYTStreamerTheme {
        PlaylistScreen()
    }
}