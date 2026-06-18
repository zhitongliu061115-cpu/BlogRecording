package com.example.blogrecording.ui

import com.example.blogrecording.common.AppError
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiNavigationPolicyTest {
    @Test
    fun openDetailSelectsSessionAndClearsError() {
        val session = fakeSession(id = "session-1")
        val segment = fakeSegment(sessionId = session.id)
        val state = AppUiState(
            currentScreen = AppScreen.HISTORY,
            error = AppError.Unknown("old error")
        )

        val updated = UiNavigationPolicy.openDetail(
            state = state,
            sessionId = session.id,
            session = session,
            segments = listOf(segment)
        )

        assertEquals(AppScreen.DETAIL, updated.currentScreen)
        assertEquals(session.id, updated.selectedSessionId)
        assertEquals(session, updated.currentSession)
        assertEquals(listOf(segment), updated.currentSegments)
        assertNull(updated.error)
    }

    @Test
    fun deleteCurrentSessionReturnsToHistoryAndClearsSelection() {
        val session = fakeSession(id = "session-2")
        val state = AppUiState(
            currentScreen = AppScreen.DETAIL,
            currentSession = session,
            currentSegments = listOf(fakeSegment(sessionId = session.id)),
            selectedSessionId = session.id
        )

        val updated = UiNavigationPolicy.deleteCurrentSession(state)

        assertEquals(AppScreen.HISTORY, updated.currentScreen)
        assertNull(updated.currentSession)
        assertNull(updated.selectedSessionId)
        assertTrue(updated.currentSegments.isEmpty())
    }

    @Test
    fun navigateChangesScreenAndClearsError() {
        val state = AppUiState(error = AppError.Unknown("old error"))

        val updated = UiNavigationPolicy.navigate(state, AppScreen.SETTINGS)

        assertEquals(AppScreen.SETTINGS, updated.currentScreen)
        assertNull(updated.error)
    }

    private fun fakeSession(id: String): RecordingSessionEntity {
        return RecordingSessionEntity(
            id = id,
            title = "Recording",
            createdAt = 1_000L,
            updatedAt = 1_000L,
            sourceType = AudioSourceType.MICROPHONE,
            status = RecordingStatus.COMPLETED,
            transcript = "transcript",
            summary = "summary",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            summaryModelName = "deepseek-chat",
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            detectedSpeakerCount = 1,
            segmentCount = 1,
            errorMessage = null
        )
    }

    private fun fakeSegment(sessionId: String): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = "segment-1",
            sessionId = sessionId,
            recordingSegmentId = null,
            startMs = 0L,
            endMs = 1_000L,
            speakerId = "speaker-1",
            speakerDisplayName = "Speaker 1",
            text = "hello",
            language = "zh",
            confidence = 0.9f,
            vadConfidence = null,
            isFinal = true,
            createdAt = 1_000L
        )
    }
}
