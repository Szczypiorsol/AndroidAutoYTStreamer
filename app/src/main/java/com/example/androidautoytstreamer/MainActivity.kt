package com.example.androidautoytstreamer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.example.androidautoytstreamer.ui.theme.AndroidAutoYTStreamerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {
    private var playbackService: PlaybackService? = null
    private var mediaSessionController: MediaSessionController? = null
    private val playlistViewModel: PlaylistViewModel by lazy {
        ViewModelProvider(this)[PlaylistViewModel::class.java]
    }
    private lateinit var authManager: GoogleAuthManager
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private val isSignedInState = mutableStateOf(false)
    private val signedInEmailState = mutableStateOf<String?>(null)
    private val playbackSnapshotState = mutableStateOf(PlaybackSnapshot())
    private var wasInCarMode = false

    private val playbackStateListener = PlaybackStateListener { snapshot ->
        playbackSnapshotState.value = snapshot
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            AppLog.d("MainActivity: PlaybackService connected")
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            playbackService?.addPlaybackStateListener(playbackStateListener)
            playbackSnapshotState.value = playbackService?.currentPlaybackSnapshot() ?: PlaybackSnapshot()
            mediaSessionController = MediaSessionController(this@MainActivity)
            maybeAutoResumeOnCarModeTransition()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            AppLog.d("MainActivity: PlaybackService disconnected")
            playbackService?.removePlaybackStateListener(playbackStateListener)
            playbackService = null
            mediaSessionController = null
            playbackSnapshotState.value = PlaybackSnapshot()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLog.initialize(this)
        PlaylistHistoryStore.initialize(filesDir)
        authManager = GoogleAuthManager(this)
        val currentAccount = authManager.getCurrentAccount()
        isSignedInState.value = currentAccount != null
        signedInEmailState.value = authManager.getCurrentAccountIdentifier()
        AppLog.d("MainActivity onCreate; signedIn=${isSignedInState.value}; account=${signedInEmailState.value}; logFile=${AppLog.filePath()}")

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            AppLog.d("Google sign-in activity result code: ${result.resultCode}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                isSignedInState.value = true
                signedInEmailState.value = account.account?.name ?: account.email
                AppLog.d("Google sign-in success for ${signedInEmailState.value ?: "unknown"} (id=${account.id})")
            } catch (e: ApiException) {
                isSignedInState.value = false
                signedInEmailState.value = null
                AppLog.e("Google sign-in failed with status code ${e.statusCode}: ${e.message}", e)
            } catch (e: Exception) {
                isSignedInState.value = false
                signedInEmailState.value = null
                AppLog.e("Google sign-in failed with error: ${e.message}", e)
            }
        }

        val intent = Intent(this, PlaybackService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        wasInCarMode = isCarModeActive(resources.configuration)

        setContent {
            AndroidAutoYTStreamerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaylistScreen(
                        modifier = Modifier.padding(innerPadding),
                        isSignedIn = isSignedInState.value,
                        signedInEmail = signedInEmailState.value,
                        logFilePath = AppLog.filePath(),
                        onPlay = { playbackService?.resume() ?: mediaSessionController?.play() },
                        onPause = { playbackService?.pause() ?: mediaSessionController?.pause() },
                        onNext = { playbackService?.next() ?: mediaSessionController?.next() },
                        onPrevious = { playbackService?.previous() ?: mediaSessionController?.previous() },
                        onOpenCurrentInYoutube = { videoId ->
                            openCurrentInYoutube(videoId)
                        },
                        onGoogleSignIn = {
                            AppLog.d("Google sign-in requested")
                            val signInIntent = authManager.signInIntent()
                            signInLauncher.launch(signInIntent)
                        },
                        onGoogleSignOut = {
                            authManager.signOut {
                                isSignedInState.value = false
                                signedInEmailState.value = null
                                AppLog.d("Google sign-out completed")
                            }
                        },
                        onQueueLoaded = { queue ->
                            AppLog.d("Queue loaded with ${queue.size} items")
                            playbackService?.setQueue(queue)
                        },
                        playbackSnapshot = playbackSnapshotState.value,
                        viewModel = playlistViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLog.d("MainActivity onResume")
        maybeAutoResumeOnCarModeTransition()
    }

    override fun onDestroy() {
        AppLog.d("MainActivity onDestroy")
        playbackService?.removePlaybackStateListener(playbackStateListener)
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun maybeAutoResumeOnCarModeTransition() {
        val isInCarMode = isCarModeActive(resources.configuration)
        val snapshot = playbackSnapshotState.value
        val shouldResume = shouldAutoResumeOnCarModeTransition(wasInCarMode, isInCarMode, snapshot)
        AppLog.d("Car mode transition check: wasInCarMode=$wasInCarMode, isInCarMode=$isInCarMode, isPlaying=${snapshot.isPlaying}, shouldResume=$shouldResume")
        if (shouldResume) {
            AppLog.d("Triggering auto-resume due to car mode transition")
            playbackService?.resume() ?: mediaSessionController?.play()
        }
        wasInCarMode = isInCarMode
    }

    private fun openCurrentInYoutube(videoId: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        runCatching {
            startActivity(intent)
            AppLog.d("Opening current video in YouTube app/browser: $videoId")
        }.onFailure {
            AppLog.e("Unable to open current video in YouTube", it)
        }
    }
}

internal fun isCarModeActive(configuration: Configuration): Boolean {
    val mode = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return mode == Configuration.UI_MODE_TYPE_CAR
}

internal fun shouldAutoResumeOnCarModeTransition(
    wasInCarMode: Boolean,
    isInCarMode: Boolean,
    snapshot: PlaybackSnapshot
): Boolean {
    if (wasInCarMode || !isInCarMode) return false
    if (snapshot.isPlaying) return false
    if (snapshot.queueEnded) return false
    return snapshot.currentVideo != null
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    AndroidAutoYTStreamerTheme {
        PlaylistScreen()
    }
}