package com.example.blogrecording.ui

import com.example.blogrecording.common.AppError
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus
import com.example.blogrecording.data.RecordingStatus

internal object RecordingLifecyclePolicy {
    fun deniedPermissionState(error: AppError): RecordingLifecycleState {
        return RecordingLifecycleState(recordingStatus = RecordingStatus.ERROR, error = error)
    }

    fun modelGateError(settings: AppSettings, status: ModelStatus): AppError? {
        return when {
            status.vad == ModelLoadStatus.MISSING && settings.enableVad -> AppError.VadModelMissing
            status.senseVoice == ModelLoadStatus.MISSING -> AppError.SenseVoiceModelMissing
            status.diarization == ModelLoadStatus.MISSING && settings.enableSpeakerDiarization ->
                AppError.DiarizationModelMissing
            else -> null
        }
    }

    fun captureFailureState(error: AppError): RecordingLifecycleState {
        return if (error == AppError.InternalAudioSilent) {
            RecordingLifecycleState(
                recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                vadLabel = "等待可捕获的系统声音",
                error = null,
                persistFailure = false
            )
        } else {
            RecordingLifecycleState(
                recordingStatus = RecordingStatus.ERROR,
                error = error,
                persistFailure = true
            )
        }
    }
}

internal data class RecordingLifecycleState(
    val recordingStatus: RecordingStatus,
    val vadLabel: String? = null,
    val error: AppError?,
    val persistFailure: Boolean = true
)
