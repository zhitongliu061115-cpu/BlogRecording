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
import com.example.blogrecording.ui.HomeScreen
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import com.example.blogrecording.ui.state.RecordingActionState
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
                onToggleHighlightFavorite = {},
                onSaveExport = {},
                onShareExport = {},
                onAskQuestion = {},
                onRetryQuestion = {},
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
                onToggleHighlightFavorite = {},
                onSaveExport = {},
                onShareExport = {},
                onAskQuestion = {},
                onRetryQuestion = {},
                onDelete = {}
            )
        }

        composeRule.onNodeWithText("正在转文字").assertExists()
        composeRule.onNodeWithText("第 2 批 1/3").assertExists()
    }

    @Test
    fun homeHeaderSettingsInvokesNavigationCallback() {
        var destination: AppScreen? = null

        composeRule.setContent {
            HomeScreen(
                state = AppUiState(
                    home = HomeUiState(
                        cards = listOf(fakePodcastCard()),
                        isEmpty = false
                    )
                ),
                onCreateSession = {},
                onImportLocalMedia = {},
                onImportUrlMedia = { _ -> },
                onStartInternal = {},
                onStartMicrophone = {},
                onStartInternalSession = {},
                onStartMicrophoneSession = {},
                onPauseRecording = {},
                onResumeInternalSession = {},
                onResumeMicrophoneSession = {},
                onFinishSession = {},
                onRequestRename = {},
                onRenameSession = { _, _ -> },
                onDismissRename = {},
                onStartSummary = {},
                onOpenDetail = {},
                onNavigate = { destination = it }
            )
        }

        composeRule.onNodeWithText("设置").performClick()

        assertEquals(AppScreen.SETTINGS, destination)
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

    private fun fakePodcastCard(): PodcastCardUiState {
        return PodcastCardUiState(
            sessionId = "podcast-1",
            title = "Header Navigation Test",
            statusLabel = "可录制",
            durationLabel = "0:00",
            segmentCountLabel = "0 段",
            transcriptionLabel = "暂无转写内容",
            transcriptPreviewSnippets = emptyList(),
            summaryLabel = "没有可总结的转写",
            isRecording = false,
            actionState = RecordingActionState(
                canStart = true,
                canPause = false,
                canResume = false,
                switchingFromAnotherSession = false
            ),
            canRename = true,
            canFinish = false,
            canStartSummary = false,
            startSummaryDisabledReason = "没有可总结的转写"
        )
    }
}
