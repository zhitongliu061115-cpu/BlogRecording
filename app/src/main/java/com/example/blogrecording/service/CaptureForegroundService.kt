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

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requestId = intent?.getStringExtra(EXTRA_START_REQUEST_ID)
        foregroundServiceType = intent?.getIntExtra(EXTRA_FOREGROUND_SERVICE_TYPE, foregroundServiceType)
            ?: foregroundServiceType
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    foregroundServiceType
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
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

    companion object {
        const val CHANNEL_ID = AndroidPlatformContract.CAPTURE_CHANNEL_ID
        const val NOTIFICATION_ID = AndroidPlatformContract.CAPTURE_NOTIFICATION_ID
        const val EXTRA_FOREGROUND_SERVICE_TYPE = AndroidPlatformContract.EXTRA_FOREGROUND_SERVICE_TYPE
        private const val EXTRA_START_REQUEST_ID = "start_request_id"
        private const val START_TIMEOUT_MS = 10_000L
        private val pendingStarts = ConcurrentHashMap<String, CompletableDeferred<StartResult>>()

        suspend fun startAndWait(context: Context, foregroundServiceType: Int): AppError? {
            val requestId = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<StartResult>()
            pendingStarts[requestId] = deferred
            val intent = Intent(context, CaptureForegroundService::class.java)
                .putExtra(EXTRA_FOREGROUND_SERVICE_TYPE, foregroundServiceType)
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
