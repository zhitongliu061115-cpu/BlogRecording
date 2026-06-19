package com.example.blogrecording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.blogrecording.R
import com.example.blogrecording.common.AppError
import com.example.blogrecording.platform.AndroidPlatformContract
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class CaptureForegroundService : Service() {
    private var foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    private var notificationState = CaptureNotificationState()

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requestId = intent?.getStringExtra(EXTRA_START_REQUEST_ID)
        foregroundServiceType = intent?.getIntExtra(EXTRA_FOREGROUND_SERVICE_TYPE, foregroundServiceType)
            ?: foregroundServiceType
        notificationState = CaptureNotificationState.from(intent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(notificationState),
                    foregroundServiceType
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(notificationState))
            }
            completeStart(requestId, null)
        } catch (error: SecurityException) {
            completeStart(requestId, AppError.ForegroundServiceStartFailed)
            stopSelf()
            return START_NOT_STICKY
        } catch (error: Exception) {
            completeStart(requestId, AppError.ForegroundServiceStartFailed)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "录音与本地转写",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示 Podcast Recap Local ASR 正在录音或转写"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("正在录音 / 转写")
            .setContentText("音频仅在本机处理，不上传原始音频。")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun buildNotification(state: CaptureNotificationState): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(state.titleText())
            .setContentText(state.bodyText())
            .setOngoing(state.recordingState != CaptureNotificationState.STATE_PAUSED)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = AndroidPlatformContract.CAPTURE_CHANNEL_ID
        const val NOTIFICATION_ID = AndroidPlatformContract.CAPTURE_NOTIFICATION_ID
        const val EXTRA_FOREGROUND_SERVICE_TYPE = AndroidPlatformContract.EXTRA_FOREGROUND_SERVICE_TYPE
        const val EXTRA_PODCAST_TITLE = AndroidPlatformContract.EXTRA_PODCAST_TITLE
        const val EXTRA_CAPTURE_SOURCE = AndroidPlatformContract.EXTRA_CAPTURE_SOURCE
        const val EXTRA_RECORDING_STATE = AndroidPlatformContract.EXTRA_RECORDING_STATE
        const val EXTRA_STAGE_TEXT = "processing_stage_text"
        const val EXTRA_ACTIVE_SESSION_ID = AndroidPlatformContract.EXTRA_ACTIVE_SESSION_ID
        const val ACTION_PAUSE_CAPTURE = AndroidPlatformContract.ACTION_PAUSE_CAPTURE
        const val ACTION_RESUME_CAPTURE = AndroidPlatformContract.ACTION_RESUME_CAPTURE
        const val ACTION_FINISH_CAPTURE = AndroidPlatformContract.ACTION_FINISH_CAPTURE
        private const val EXTRA_START_REQUEST_ID = "start_request_id"
        private const val START_TIMEOUT_MS = 10_000L
        private val pendingStarts = ConcurrentHashMap<String, CompletableDeferred<StartResult>>()

        suspend fun startAndWait(
            context: Context,
            foregroundServiceType: Int,
            notificationState: CaptureNotificationState = CaptureNotificationState()
        ): AppError? {
            val requestId = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<StartResult>()
            pendingStarts[requestId] = deferred
            val intent = buildStartIntent(context, foregroundServiceType, notificationState)
                .putExtra(EXTRA_START_REQUEST_ID, requestId)
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                when (val result = withTimeoutOrNull(START_TIMEOUT_MS) { deferred.await() }) {
                    StartResult.Success -> null
                    is StartResult.Failure -> result.error
                    null -> AppError.ForegroundServiceStartFailed
                }
            } catch (error: SecurityException) {
                AppError.ForegroundServiceStartFailed
            } catch (error: Exception) {
                AppError.ForegroundServiceStartFailed
            } finally {
                pendingStarts.remove(requestId)
            }
        }

        fun buildStartIntent(
            context: Context,
            foregroundServiceType: Int,
            notificationState: CaptureNotificationState = CaptureNotificationState()
        ): Intent {
            return Intent(context, CaptureForegroundService::class.java)
                .putExtra(EXTRA_FOREGROUND_SERVICE_TYPE, foregroundServiceType)
                .putExtra(EXTRA_PODCAST_TITLE, notificationState.podcastTitle)
                .putExtra(EXTRA_CAPTURE_SOURCE, notificationState.captureSource)
                .putExtra(EXTRA_RECORDING_STATE, notificationState.recordingState)
                .putExtra(EXTRA_STAGE_TEXT, notificationState.stageText)
                .putExtra(EXTRA_ACTIVE_SESSION_ID, notificationState.activeSessionId)
        }

        private fun completeStart(requestId: String?, error: AppError?) {
            if (requestId == null) return
            val result = if (error == null) StartResult.Success else StartResult.Failure(error)
            pendingStarts.remove(requestId)?.complete(result)
        }

        private sealed class StartResult {
            data object Success : StartResult()
            data class Failure(val error: AppError) : StartResult()
        }
    }
}

data class CaptureNotificationState(
    val podcastTitle: String? = null,
    val captureSource: String? = null,
    val recordingState: String? = null,
    val stageText: String? = null,
    val activeSessionId: String? = null
) {
    fun titleText(): String {
        return podcastTitle?.takeIf { it.isNotBlank() } ?: "Podcast recording"
    }

    fun bodyText(): String {
        val source = captureSource.sourceLabel()
        val stage = stageText?.takeIf { it.isNotBlank() }
        if (stage != null) return "$source：$stage"
        val state = recordingState.stateLabel()
        return "$source：$state，本机处理"
    }

    private fun String?.sourceLabel(): String {
        return when (this) {
            SOURCE_MICROPHONE -> "麦克风"
            SOURCE_SYSTEM_AUDIO -> "系统内录"
            else -> "音频"
        }
    }

    private fun String?.stateLabel(): String {
        return when (this) {
            STATE_PAUSED -> "已暂停"
            STATE_PROCESSING -> "处理中"
            STATE_SUMMARIZING -> "总结中"
            else -> "录制中"
        }
    }

    companion object {
        const val SOURCE_MICROPHONE = "microphone"
        const val SOURCE_SYSTEM_AUDIO = "system-audio"
        const val SOURCE_AUDIO = "audio"
        const val STATE_RECORDING = "recording"
        const val STATE_PAUSED = "paused"
        const val STATE_PROCESSING = "processing"
        const val STATE_SUMMARIZING = "summarizing"

        fun from(intent: Intent?): CaptureNotificationState {
            return CaptureNotificationState(
                podcastTitle = intent?.getStringExtra(CaptureForegroundService.EXTRA_PODCAST_TITLE),
                captureSource = intent?.getStringExtra(CaptureForegroundService.EXTRA_CAPTURE_SOURCE),
                recordingState = intent?.getStringExtra(CaptureForegroundService.EXTRA_RECORDING_STATE),
                stageText = intent?.getStringExtra(CaptureForegroundService.EXTRA_STAGE_TEXT),
                activeSessionId = intent?.getStringExtra(CaptureForegroundService.EXTRA_ACTIVE_SESSION_ID)
            )
        }
    }
}
