package com.example.blogrecording.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class InternalAudioCaptureManager(
    private val mediaProjection: MediaProjection?,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) : AudioCaptureManager {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<AppResult<PcmAudioStream>> = callbackFlow {
        val projection = mediaProjection
        if (projection == null) {
            trySend(AppResult.Failure(AppError.MediaProjectionDenied))
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

        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val record = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuffer * 2)
            .setAudioPlaybackCaptureConfig(captureConfig)
            .build()

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
                var silentReads = 0
                while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        val samples = buffer.copyOf(count)
                        if (samples.all { it == 0.toShort() }) {
                            silentReads += 1
                            if (silentReads >= SILENT_READ_LIMIT) {
                                trySend(AppResult.Failure(AppError.InternalAudioSilent))
                                silentReads = 0
                            }
                        } else {
                            silentReads = 0
                        }
                        trySend(
                            AppResult.Success(
                                PcmAudioStream(
                                    samples = samples,
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
        reader.name = "internal-audio-reader"
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
        runCatching { mediaProjection?.stop() }
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000
        const val SILENT_READ_LIMIT = 80
    }
}
