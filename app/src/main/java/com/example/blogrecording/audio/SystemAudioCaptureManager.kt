package com.example.blogrecording.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SystemAudioCaptureManager(
    private val context: Context,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) : AudioCaptureManager {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<AppResult<PcmAudioStream>> = callbackFlow {
        if (!SystemAudioCapturePermissionPolicy.hasPrivilegedCapturePermission(context)) {
            Log.w(TAG, "start_denied missing_privileged_output_capture_permission")
            trySend(AppResult.Failure(AppError.SystemAudioCapturePermissionDenied))
            close()
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.i(TAG, "min_buffer size=$minBuffer source=REMOTE_SUBMIX")
        if (minBuffer <= 0) {
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("minBuffer=$minBuffer")))
            close()
            return@callbackFlow
        }

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val record = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuffer * 2)
                .build()
        } catch (error: Throwable) {
            Log.w(TAG, "build_failed source=REMOTE_SUBMIX error=${error.javaClass.simpleName}")
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("REMOTE_SUBMIX:${error.javaClass.simpleName}")))
            close()
            return@callbackFlow
        }

        Log.i(TAG, "record_built source=REMOTE_SUBMIX state=${record.state}")
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("REMOTE_SUBMIX_STATE_UNINITIALIZED")))
            close()
            return@callbackFlow
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (error: Throwable) {
            Log.w(TAG, "start_recording_failed source=REMOTE_SUBMIX error=${error.javaClass.simpleName}")
            record.release()
            audioRecord = null
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("REMOTE_SUBMIX:${error.javaClass.simpleName}")))
            close()
            return@callbackFlow
        }

        Log.i(TAG, "record_started source=REMOTE_SUBMIX recordingState=${record.recordingState}")
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            audioRecord = null
            trySend(AppResult.Failure(AppError.AudioRecordInitFailed("REMOTE_SUBMIX_NOT_RECORDING")))
            close()
            return@callbackFlow
        }

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
                        Log.w(TAG, "read_failed source=REMOTE_SUBMIX count=$count")
                        trySend(AppResult.Failure(AppError.AudioRecordInitFailed("REMOTE_SUBMIX_READ_$count")))
                    }
                }
            } catch (_: Throwable) {
                Log.i(TAG, "reader_unwound source=REMOTE_SUBMIX")
            } finally {
                Log.i(TAG, "reader_closed source=REMOTE_SUBMIX")
                close()
            }
        }
        reader.name = "system-audio-reader"
        reader.start()

        awaitClose {
            stop()
        }
    }

    override fun stop() {
        val record = audioRecord ?: return
        Log.i(TAG, "stop_requested source=REMOTE_SUBMIX recordingState=${record.recordingState}")
        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }.onFailure { Log.w(TAG, "stop_failed source=REMOTE_SUBMIX error=${it.javaClass.simpleName}") }
        record.release()
        audioRecord = null
    }

    private companion object {
        const val TAG = "BlogRecordingSystemAudio"
        const val DEFAULT_SAMPLE_RATE = 16_000
    }
}
