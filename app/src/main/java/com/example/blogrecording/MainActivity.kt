package com.example.blogrecording

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.blogrecording.ui.AppViewModel
import com.example.blogrecording.ui.HomeScreen
import com.example.blogrecording.ui.PodcastRecapApp
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.theme.BlogRecordingTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pendingStart = remember { mutableStateOf<PendingStartAction?>(null) }
            val mediaProjectionManager = remember {
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            }
            val mediaProjectionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val data = result.data
                if (result.resultCode == RESULT_OK && data != null) {
                    when (val action = pendingStart.value) {
                        is PendingStartAction.Internal -> {
                            viewModel.startInternalRecording(result.resultCode, data, action.sessionId)
                        }
                        is PendingStartAction.Microphone,
                        null -> viewModel.startInternalRecording(result.resultCode, data)
                    }
                } else {
                    viewModel.onMediaProjectionDenied()
                }
                pendingStart.value = null
            }
            val capturePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true
                val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    grants[Manifest.permission.POST_NOTIFICATIONS] == true
                } else {
                    true
                }
                when {
                    !audioGranted -> viewModel.onRecordAudioPermissionDenied()
                    !notificationGranted -> viewModel.onNotificationPermissionDenied()
                    else -> when (val action = pendingStart.value) {
                        is PendingStartAction.Internal -> {
                            mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                        is PendingStartAction.Microphone -> viewModel.startMicrophoneRecording(action.sessionId)
                        null -> Unit
                    }
                }
                if (pendingStart.value !is PendingStartAction.Internal) {
                    pendingStart.value = null
                }
            }
            val state by viewModel.state.collectAsState()
            BlogRecordingTheme {
                PodcastRecapApp(
                    state = state,
                    viewModel = viewModel,
                    onStartInternal = {
                        pendingStart.value = PendingStartAction.Internal(sessionId = null)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onStartInternalSession = { sessionId ->
                        pendingStart.value = PendingStartAction.Internal(sessionId)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onResumeInternalSession = { sessionId ->
                        pendingStart.value = PendingStartAction.Internal(sessionId)
                        capturePermissionLauncher.launch(capturePermissions())
                    }
                )
            }
        }
    }

    private fun capturePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private sealed interface PendingStartAction {
    data class Internal(val sessionId: String?) : PendingStartAction
    data class Microphone(val sessionId: String?) : PendingStartAction
}

@Composable
fun AppPreview() {
    BlogRecordingTheme {
        HomeScreen(
            state = AppUiState(),
            onCreateSession = {},
            onStartInternal = {},
            onStartInternalSession = {},
            onPauseRecording = {},
            onResumeInternalSession = {},
            onFinishSession = {},
            onRequestRename = {},
            onRenameSession = { _, _ -> },
            onDismissRename = {},
            onStartSummary = {},
            onOpenDetail = {},
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreviewLight() {
    AppPreview()
}
