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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidautoytstreamer.ui.theme.AndroidAutoYTStreamerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {
    private var playbackService: PlaybackService? = null
    private var mediaSessionController: MediaSessionController? = null
    private lateinit var authManager: GoogleAuthManager
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private val isSignedInState = mutableStateOf(false)

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
        authManager = GoogleAuthManager(this)
        isSignedInState.value = authManager.getCurrentAccount() != null
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    isSignedInState.value = true
                    println("Google account: ${account.email}")
                }
            } catch (_: ApiException) {
                isSignedInState.value = false
                println("Google sign-in failed")
            }
        }

        val intent = Intent(this, PlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            AndroidAutoYTStreamerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaylistScreen(
                        modifier = Modifier.padding(innerPadding),
                        isSignedIn = isSignedInState.value,
                        onPlay = { playbackService?.resume() ?: mediaSessionController?.play() },
                        onPause = { playbackService?.pause() ?: mediaSessionController?.pause() },
                        onNext = { playbackService?.next() ?: mediaSessionController?.next() },
                        onPrevious = { playbackService?.previous() ?: mediaSessionController?.previous() },
                        onGoogleSignIn = {
                            val signInIntent = authManager.signInIntent()
                            signInLauncher.launch(signInIntent)
                        },
                        onGoogleSignOut = {
                            authManager.signOut {
                                isSignedInState.value = false
                            }
                        },
                        onQueueLoaded = { queue ->
                            playbackService?.setQueue(queue)
                        }
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