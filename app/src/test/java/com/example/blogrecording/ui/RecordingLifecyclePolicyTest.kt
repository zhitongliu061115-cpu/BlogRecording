package com.example.blogrecording.ui

import com.example.blogrecording.common.AppError
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus
import com.example.blogrecording.data.RecordingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingLifecyclePolicyTest {
    @Test
    fun deniedPermissionsMoveStateToError() {
        val audioDenied = RecordingLifecyclePolicy.deniedPermissionState(AppError.RecordAudioPermissionDenied)
        val notificationDenied = RecordingLifecyclePolicy.deniedPermissionState(AppError.NotificationPermissionDenied)

        assertEquals(RecordingStatus.ERROR, audioDenied.recordingStatus)
        assertEquals(AppError.RecordAudioPermissionDenied, audioDenied.error)
        assertEquals(RecordingStatus.ERROR, notificationDenied.recordingStatus)
        assertEquals(AppError.NotificationPermissionDenied, notificationDenied.error)
    }

    @Test
    fun modelGateRespectsEnabledSettings() {
        val missingAll = ModelStatus(
            senseVoice = ModelLoadStatus.MISSING,
            vad = ModelLoadStatus.MISSING,
            diarization = ModelLoadStatus.MISSING
        )

        assertEquals(
            AppError.VadModelMissing,
            RecordingLifecyclePolicy.modelGateError(AppSettings(), missingAll)
        )
        assertEquals(
            AppError.SenseVoiceModelMissing,
            RecordingLifecyclePolicy.modelGateError(AppSettings(enableVad = false), missingAll)
        )
        assertEquals(
            AppError.DiarizationModelMissing,
            RecordingLifecyclePolicy.modelGateError(
                AppSettings(enableVad = false),
                missingAll.copy(senseVoice = ModelLoadStatus.LOADED)
            )
        )
        assertNull(
            RecordingLifecyclePolicy.modelGateError(
                AppSettings(enableVad = false, enableSpeakerDiarization = false),
                missingAll.copy(senseVoice = ModelLoadStatus.LOADED)
            )
        )
    }

    @Test
    fun internalAudioSilenceIsRecoverableWithoutPersistingFailure() {
        val state = RecordingLifecyclePolicy.captureFailureState(AppError.InternalAudioSilent)

        assertEquals(RecordingStatus.CAPTURING_AUDIO, state.recordingStatus)
        assertFalse(state.persistFailure)
        assertNull(state.error)
        assertTrue(state.vadLabel!!.contains("系统声音"))
    }

    @Test
    fun nonRecoverableCaptureFailurePersistsError() {
        val state = RecordingLifecyclePolicy.captureFailureState(AppError.ForegroundServiceStartFailed)

        assertEquals(RecordingStatus.ERROR, state.recordingStatus)
        assertTrue(state.persistFailure)
        assertEquals(AppError.ForegroundServiceStartFailed, state.error)
    }
}
