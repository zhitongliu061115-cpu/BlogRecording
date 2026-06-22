package com.example.blogrecording

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.ui.AiChatScreen
import com.example.blogrecording.ui.DetailScreen
import com.example.blogrecording.ui.HistoryScreen
import com.example.blogrecording.ui.HomeScreen
import com.example.blogrecording.ui.MineScreen
import com.example.blogrecording.ui.state.AiChatMessageUiState
import com.example.blogrecording.ui.state.AiChatSender
import com.example.blogrecording.ui.state.AiChatUiState
import com.example.blogrecording.ui.state.AiPodcastCardUiState
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

        composeRule.onNodeWithText("删除").performScrollTo().performClick()

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

        composeRule.onNodeWithContentDescription("设置").performClick()

        assertEquals(AppScreen.SETTINGS, destination)
    }

    @Test
    fun mineEntriesInvokeNavigationCallbacks() {
        var destination: AppScreen? = null

        composeRule.setContent {
            MineScreen(
                state = AppUiState(
                    home = HomeUiState(
                        cards = listOf(fakePodcastCard()),
                        isEmpty = false
                    )
                ),
                onNavigate = { destination = it }
            )
        }

        composeRule.onNodeWithTag("mine-entry-设置").performClick()
        assertEquals(AppScreen.SETTINGS, destination)

        composeRule.onNodeWithTag("mine-entry-历史").performClick()
        assertEquals(AppScreen.HISTORY, destination)
    }

    @Test
    fun aiPodcastCardAndNewConversationInvokeCallbacks() {
        var selectedSessionId: String? = null
        var newConversationRequested = false

        composeRule.setContent {
            AiChatScreen(
                state = AiChatUiState(
                    isChoosingPodcast = true,
                    cards = listOf(fakeAiPodcastCard())
                ),
                onSelectPodcast = { selectedSessionId = it },
                onNewConversation = { newConversationRequested = true },
                onDraftChange = {},
                onSend = {},
                onRetryQuestion = {}
            )
        }

        composeRule.onNodeWithTag("ai-podcast-card-podcast-1").performClick()
        assertEquals("podcast-1", selectedSessionId)

        composeRule.onNodeWithTag("ai-new-conversation").performClick()
        assertTrue(newConversationRequested)
    }

    @Test
    fun aiChatInputInvokesDraftAndSendCallbacks() {
        var draft = ""
        var sent = false

        composeRule.setContent {
            AiChatScreen(
                state = AiChatUiState(
                    selectedSessionId = "podcast-1",
                    isChoosingPodcast = false,
                    draftQuestion = "问题",
                    cards = listOf(fakeAiPodcastCard()),
                    messages = listOf(
                        AiChatMessageUiState(
                            id = "assistant-1",
                            text = "可以开始聊这期播客。",
                            sender = AiChatSender.ASSISTANT,
                            timestampLabel = "10:00"
                        )
                    )
                ),
                onSelectPodcast = {},
                onNewConversation = {},
                onDraftChange = { draft = it },
                onSend = { sent = true },
                onRetryQuestion = {}
            )
        }

        composeRule.onNodeWithTag("ai-chat-input").performTextInput("补充")
        assertTrue(draft.contains("补充"))

        composeRule.onNodeWithTag("ai-send").performClick()
        assertTrue(sent)
    }

    @Test
    fun aiFailedMessageInvokesRetryCallback() {
        var retriedMessageId: String? = null

        composeRule.setContent {
            AiChatScreen(
                state = AiChatUiState(
                    selectedSessionId = "podcast-1",
                    isChoosingPodcast = false,
                    cards = listOf(fakeAiPodcastCard()),
                    messages = listOf(
                        AiChatMessageUiState(
                            id = "answer-failed-1",
                            text = "DeepSeek QA request failed",
                            sender = AiChatSender.ASSISTANT,
                            timestampLabel = "10:01",
                            statusLabel = "可重试",
                            retryMessageId = "failed-1",
                            isError = true
                        )
                    )
                ),
                onSelectPodcast = {},
                onNewConversation = {},
                onDraftChange = {},
                onSend = {},
                onRetryQuestion = { retriedMessageId = it }
            )
        }

        composeRule.onNodeWithTag("ai-retry-failed-1").performClick()

        assertEquals("failed-1", retriedMessageId)
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
            summaryStyle = SummaryStyle.BULLET_SUMMARY,
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

    private fun fakeAiPodcastCard(): AiPodcastCardUiState {
        return AiPodcastCardUiState(
            sessionId = "podcast-1",
            title = "AI Callback Episode",
            statusLabel = "可总结",
            durationLabel = "1:03",
            transcriptionLabel = "已转写 2 段",
            summaryLabel = "可总结",
            tagLabels = listOf("AI"),
            transcriptPreview = "hello world"
        )
    }
}
