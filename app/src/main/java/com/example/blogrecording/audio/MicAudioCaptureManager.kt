package com.example.blogrecording.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MicAudioCaptureManager(
    private val context: Context,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) : AudioCaptureManager {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<AppResult<PcmAudioStream>> = callbackFlow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            trySend(AppResult.Failure(AppError.RecordAudioPermissionDenied))
            close()
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("minBuffer=$minBuffer")))
            close()
            return@callbackFlow
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("STATE_UNINITIALIZED")))
            close()
            return@callbackFlow
        }

        audioRecord = record
        record.startRecording()
        val buffer = ShortArray(minBuffer / 2)
        val reader = Thread {
            try {
                while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        trySend(
                            AppResult.Success(
                                PcmAudioStream(
                                    samples = buffer.copyOf(count),
                                    sampleRate = sampleRate,
                                    channelCount = 1,
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        )
                    } else if (count < 0) {
                        trySend(AppResult.Failure(AppError.AudioRecordInitFailed("read=$count")))
                    }
                }
            } catch (_: Throwable) {
                // stop() may release AudioRecord while the reader thread is unwinding.
            } finally {
                close()
            }
        }
        reader.name = "mic-audio-reader"
        reader.start()

        awaitClose {
            stop()
        }
    }

    override fun stop() {
        val record = audioRecord ?: return
        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }
        record.release()
        audioRecord = null
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000
    }
}
