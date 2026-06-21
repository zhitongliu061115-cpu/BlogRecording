package com.example.blogrecording.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.util.Log
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class InternalAudioCaptureManager(
    private val mediaProjection: MediaProjection?,
    private val context: Context? = null,
    private val preferredCaptureUids: List<Int> = emptyList(),
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) : AudioCaptureManager {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<AppResult<PcmAudioStream>> = callbackFlow {
        val projection = mediaProjection
        if (projection == null) {
            Log.w(TAG, "start_denied projection=null")
            trySend(AppResult.Failure(AppError.MediaProjectionDenied))
            close()
            return@callbackFlow
        }
        Log.i(TAG, "start_requested sampleRate=$sampleRate")

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.i(TAG, "min_buffer size=$minBuffer")
        if (minBuffer <= 0) {
            Log.w(TAG, "min_buffer_invalid size=$minBuffer")
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("minBuffer=$minBuffer")))
            close()
            return@callbackFlow
        }

        val captureConfig = buildCaptureConfig(projection)

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val record = try {
            AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuffer * 2)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
        } catch (error: Throwable) {
            Log.w(TAG, "build_failed error=${error.javaClass.simpleName}")
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed(error.javaClass.simpleName)))
            close()
            return@callbackFlow
        }

        Log.i(TAG, "record_built state=${record.state}")
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "record_uninitialized state=${record.state}")
            record.release()
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("STATE_UNINITIALIZED")))
            close()
            return@callbackFlow
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (error: Throwable) {
            Log.w(TAG, "start_recording_failed error=${error.javaClass.simpleName}")
            record.release()
            audioRecord = null
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed(error.javaClass.simpleName)))
            close()
            return@callbackFlow
        }
        Log.i(TAG, "record_started recordingState=${record.recordingState}")
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.w(TAG, "record_not_recording recordingState=${record.recordingState}")
            record.release()
            audioRecord = null
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("RECORDSTATE_NOT_RECORDING")))
            close()
            return@callbackFlow
        }
        val buffer = ShortArray(minBuffer / 2)
        val reader = Thread {
            try {
                var silentReads = 0
                var totalReads = 0
                var firstNonZeroLogged = false
                while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        totalReads += 1
                        val samples = buffer.copyOf(count)
                        if (samples.all { it == 0.toShort() }) {
                            silentReads += 1
                            if (silentReads == 1 || silentReads % SILENT_LOG_INTERVAL == 0) {
                                Log.i(TAG, "read_silence totalReads=$totalReads silentReads=$silentReads samples=$count")
                            }
                            if (silentReads >= SILENT_READ_LIMIT) {
                                Log.w(TAG, "read_silence_limit totalReads=$totalReads silentReads=$silentReads")
                                trySend(AppResult.Failure(AppError.InternalAudioSilent))
                                silentReads = 0
                            }
                        } else {
                            if (!firstNonZeroLogged) {
                                Log.i(TAG, "read_non_zero_first totalReads=$totalReads avgAmp=${averageAmplitude(samples).toInt()} samples=$count")
                                firstNonZeroLogged = true
                            }
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
                        Log.w(TAG, "read_failed count=$count")
                        trySend(AppResult.Failure(AppError.AudioRecordInitFailed("read=$count")))
                    }
                }
            } catch (_: Throwable) {
                Log.i(TAG, "reader_unwound")
                // stop() may release AudioRecord while the reader thread is unwinding.
            } finally {
                Log.i(TAG, "reader_closed")
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
        Log.i(TAG, "stop_requested recordingState=${record.recordingState}")
        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }.onFailure { Log.w(TAG, "stop_failed error=${it.javaClass.simpleName}") }
        record.release()
        audioRecord = null
        runCatching { mediaProjection?.stop() }
            .onFailure { Log.w(TAG, "projection_stop_failed error=${it.javaClass.simpleName}") }
    }

    private fun averageAmplitude(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0L
        samples.forEach { sample -> sum += kotlin.math.abs(sample.toInt()) }
        return sum.toDouble() / samples.size
    }

    private fun buildCaptureConfig(projection: MediaProjection): AudioPlaybackCaptureConfiguration {
        val builder = AudioPlaybackCaptureConfiguration.Builder(projection)
        val matchingUids = preferredCaptureUids()
        if (matchingUids.isNotEmpty()) {
            matchingUids.forEach { uid -> builder.addMatchingUid(uid) }
            Log.i(TAG, "capture_config mode=matching_uids matchingUids=${matchingUids.joinToString(",")}")
            return builder.build()
        }

        InternalAudioCapturePolicy.matchingUsages.forEach { usage ->
            builder.addMatchingUsage(usage)
        }
        Log.i(TAG, "capture_config mode=usages usages=${InternalAudioCapturePolicy.matchingUsages.joinToString(",")}")
        return builder.build()
    }

    private fun preferredCaptureUids(): List<Int> {
        if (preferredCaptureUids.isNotEmpty()) return preferredCaptureUids.distinct()
        val packageManager = context?.packageManager ?: return emptyList()
        return InternalAudioCapturePolicy.preferredCapturePackages.mapNotNull { packageName ->
            runCatching {
                packageManager.getApplicationInfo(packageName, 0).uid.also { uid ->
                    Log.i(TAG, "preferred_package_resolved package=$packageName uid=$uid")
                }
            }.onFailure {
                Log.i(TAG, "preferred_package_unavailable package=$packageName error=${it.javaClass.simpleName}")
            }.getOrNull()
        }.distinct()
    }

    private companion object {
        const val TAG = "BlogRecordingInternalAudio"
        const val DEFAULT_SAMPLE_RATE = 16_000
        const val SILENT_READ_LIMIT = 80
        const val SILENT_LOG_INTERVAL = 40
    }
}
