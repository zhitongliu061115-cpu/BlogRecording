package com.example.blogrecording

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
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
import androidx.lifecycle.lifecycleScope
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.export.SessionExportFormat
import com.example.blogrecording.export.SessionExportPayload
import com.example.blogrecording.ui.AppViewModel
import com.example.blogrecording.ui.HomeScreen
import com.example.blogrecording.ui.PodcastRecapApp
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.theme.BlogRecordingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pendingStart = remember { mutableStateOf<PendingStartAction?>(null) }
            val pendingExport = remember { mutableStateOf<SessionExportPayload?>(null) }
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
                    !audioGranted -> {
                        viewModel.onRecordAudioPermissionDenied()
                        pendingStart.value = null
                    }
                    !notificationGranted -> {
                        viewModel.onNotificationPermissionDenied()
                        pendingStart.value = null
                    }
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
            val localMediaLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri == null) {
                    viewModel.onLocalMediaImportCanceled()
                } else {
                    viewModel.importLocalMedia(uri)
                }
            }
            val saveExportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val uri = result.data?.data
                val payload = pendingExport.value
                when {
                    result.resultCode != RESULT_OK || uri == null -> viewModel.onExportCanceled()
                    payload != null && writeExportPayload(uri, payload) -> Unit
                    else -> viewModel.onExportWriteFailed()
                }
                pendingExport.value = null
            }
            val state by viewModel.state.collectAsState()
            BlogRecordingTheme {
                PodcastRecapApp(
                    state = state,
                    viewModel = viewModel,
                    onStartInternal = {
                        viewModel.prepareInternalAudioAuthorization()
                        pendingStart.value = PendingStartAction.Internal(sessionId = null)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onStartMicrophone = {
                        pendingStart.value = PendingStartAction.Microphone(sessionId = null)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onImportLocalMedia = {
                        localMediaLauncher.launch(arrayOf("audio/*", "video/*"))
                    },
                    onImportUrlMedia = viewModel::importUrlMedia,
                    onSaveExport = { format ->
                        buildExport(format) { payload ->
                            pendingExport.value = payload
                            saveExportLauncher.launch(payload.createDocumentIntent())
                        }
                    },
                    onShareExport = { format ->
                        buildExport(format) { payload ->
                            startActivity(payload.shareIntent())
                        }
                    },
                    onStartInternalSession = { sessionId ->
                        viewModel.prepareInternalAudioAuthorization(sessionId)
                        pendingStart.value = PendingStartAction.Internal(sessionId)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onStartMicrophoneSession = { sessionId ->
                        pendingStart.value = PendingStartAction.Microphone(sessionId)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onResumeInternalSession = { sessionId ->
                        viewModel.prepareInternalAudioAuthorization(sessionId)
                        pendingStart.value = PendingStartAction.Internal(sessionId)
                        capturePermissionLauncher.launch(capturePermissions())
                    },
                    onResumeMicrophoneSession = { sessionId ->
                        pendingStart.value = PendingStartAction.Microphone(sessionId)
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

    private fun buildExport(
        format: SessionExportFormat,
        onReady: (SessionExportPayload) -> Unit
    ) {
        lifecycleScope.launch {
            when (val result = viewModel.buildCurrentSessionExport(format)) {
                is AppResult.Success -> onReady(result.value)
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun writeExportPayload(uri: Uri, payload: SessionExportPayload): Boolean {
        return runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(payload.content.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        }.getOrDefault(false)
    }
}

private fun SessionExportPayload.createDocumentIntent(): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_TITLE, fileName)
}

private fun SessionExportPayload.shareIntent(): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_TEXT, content)
        .putExtra(Intent.EXTRA_TITLE, fileName)
    return Intent.createChooser(sendIntent, "分享导出")
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
            onImportLocalMedia = {},
            onImportUrlMedia = {},
            onStartInternal = {},
            onStartMicrophone = {},
            onStartInternalSession = {},
            onStartMicrophoneSession = {},
            onPauseRecording = {},
            onResumeInternalSession = {},
            onResumeMicrophoneSession = {},
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
