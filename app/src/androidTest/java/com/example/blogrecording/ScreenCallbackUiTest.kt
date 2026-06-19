package com.example.blogrecording

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.ui.DetailScreen
import com.example.blogrecording.ui.HistoryScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScreenCallbackUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historyClickOpensSelectedSession() {
        var openedSessionId: String? = null
        val session = fakeSession(id = "session-1", title = "Callback Test Recording")

        composeRule.setContent {
            HistoryScreen(
                state = AppUiState(sessions = listOf(session)),
                onOpenDetail = { openedSessionId = it },
                onBack = {}
            )
        }

        composeRule.onNodeWithText("Callback Test Recording").performClick()

        assertEquals("session-1", openedSessionId)
    }

    @Test
    fun detailDeleteInvokesDeleteCallback() {
        var deleted = false

        composeRule.setContent {
            DetailScreen(
                state = AppUiState(currentSession = fakeSession(title = "Delete Test Recording")),
                onBack = {},
                onGenerateSummary = {},
                onDelete = { deleted = true }
            )
        }

        composeRule.onNodeWithText("删除").performClick()

        assertTrue(deleted)
    }

    @Test
    fun detailShowsProcessingStageFeedback() {
        composeRule.setContent {
            DetailScreen(
                state = AppUiState(
                    currentSession = fakeSession(title = "Stage Test Recording"),
                    processingStage = ProcessingStageUiState.transcribing(
                        chunkSequence = 2,
                        segmentIndex = 1,
                        segmentCount = 3
                    )
                ),
                onBack = {},
                onGenerateSummary = {},
                onDelete = {}
            )
        }

        composeRule.onNodeWithText("正在转文字").assertExists()
        composeRule.onNodeWithText("第 2 批 1/3").assertExists()
    }

    private fun fakeSession(
        id: String = "session",
        title: String = "Recording"
    ): RecordingSessionEntity {
        return RecordingSessionEntity(
            id = id,
            title = title,
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
}
