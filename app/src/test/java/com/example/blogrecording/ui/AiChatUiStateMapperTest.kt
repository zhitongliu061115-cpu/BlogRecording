package com.example.blogrecording.ui

import com.example.blogrecording.data.QaMessageStatus
import com.example.blogrecording.data.SessionQaMessage
import com.example.blogrecording.ui.state.AiChatMessageUiState
import com.example.blogrecording.ui.state.AiChatSender
import com.example.blogrecording.ui.state.AiChatUiState
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.RecordingActionState
import com.example.blogrecording.ui.state.TranscriptPreviewSnippet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatUiStateMapperTest {
    @Test
    fun syncFromHomeBuildsPodcastCardsAndKeepsChooserOpenOnFirstEntry() {
        val state = AiChatUiStateMapper.syncFromHome(
            current = AiChatUiState(),
            home = HomeUiState(cards = listOf(card()), isEmpty = false)
        )

        assertTrue(state.isChoosingPodcast)
        assertEquals("session-1", state.cards.single().sessionId)
        assertEquals("Episode A", state.cards.single().title)
        assertEquals("hello\nworld", state.cards.single().transcriptPreview)
    }

    @Test
    fun selectPodcastStartsEmptyPersistedConversationView() {
        val synced = AiChatUiStateMapper.syncFromHome(
            current = AiChatUiState(),
            home = HomeUiState(cards = listOf(card()), isEmpty = false)
        )

        val selected = AiChatUiStateMapper.selectPodcast(synced, "session-1")

        assertEquals("session-1", selected.selectedSessionId)
        assertFalse(selected.isChoosingPodcast)
        assertTrue(selected.messages.isEmpty())
    }

    @Test
    fun startNewConversationClearsSelectedPodcastAndMessages() {
        val selected = AiChatUiState(
            selectedSessionId = "session-1",
            isChoosingPodcast = false,
            messages = listOf(
                AiChatMessageUiState(
                    id = "message",
                    text = "hello",
                    sender = AiChatSender.USER,
                    timestampLabel = "10:00"
                )
            ),
            draftQuestion = "question",
            isAsking = true
        )

        val reset = AiChatUiStateMapper.startNewConversation(selected)

        assertEquals(null, reset.selectedSessionId)
        assertTrue(reset.isChoosingPodcast)
        assertTrue(reset.messages.isEmpty())
        assertEquals("", reset.draftQuestion)
        assertFalse(reset.isAsking)
    }

    @Test
    fun sendDraftClearsInputAndWaitsForPersistedQaHistory() {
        val synced = AiChatUiStateMapper.syncFromHome(
            current = AiChatUiState(draftQuestion = "  本期重点是什么？  "),
            home = HomeUiState(cards = listOf(card()), isEmpty = false)
        )
        val selected = AiChatUiStateMapper.selectPodcast(
            current = synced,
            sessionId = "session-1"
        )
        val withDraft = AiChatUiStateMapper.updateDraft(selected, "  本期重点是什么？  ")

        val sent = AiChatUiStateMapper.sendDraft(withDraft)

        assertEquals("", sent.draftQuestion)
        assertTrue(sent.messages.isEmpty())
    }

    @Test
    fun syncQaHistoryMapsAnsweredMessageToUserAndAssistantBubbles() {
        val state = AiChatUiStateMapper.syncQaHistory(
            current = AiChatUiState(selectedSessionId = "session-1"),
            messages = listOf(
                qaMessage(
                    id = "qa-1",
                    question = "重点？",
                    answer = "重点是复盘。",
                    status = QaMessageStatus.ANSWERED
                )
            ),
            isAsking = false
        )

        assertEquals(2, state.messages.size)
        assertEquals(AiChatSender.USER, state.messages[0].sender)
        assertEquals("重点？", state.messages[0].text)
        assertEquals(AiChatSender.ASSISTANT, state.messages[1].sender)
        assertEquals("重点是复盘。", state.messages[1].text)
    }

    @Test
    fun syncQaHistoryMapsFailedMessageWithRetryId() {
        val state = AiChatUiStateMapper.syncQaHistory(
            current = AiChatUiState(selectedSessionId = "session-1"),
            messages = listOf(
                qaMessage(
                    id = "qa-2",
                    question = "重点？",
                    answer = null,
                    status = QaMessageStatus.FAILED,
                    errorMessage = "DeepSeek QA request failed"
                )
            ),
            isAsking = false
        )

        val error = state.messages.last()
        assertTrue(error.isError)
        assertEquals("qa-2", error.retryMessageId)
        assertEquals("可重试", error.statusLabel)
        assertEquals("DeepSeek QA request failed", error.text)
    }

    private fun card(): PodcastCardUiState {
        return PodcastCardUiState(
            sessionId = "session-1",
            title = "Episode A",
            statusLabel = "可总结",
            durationLabel = "1:03",
            segmentCountLabel = "2 段",
            transcriptionLabel = "已转写 2 段",
            transcriptPreviewSnippets = listOf(
                TranscriptPreviewSnippet("0:01", "hello"),
                TranscriptPreviewSnippet("0:02", "world")
            ),
            tagLabels = listOf("AI", "播客"),
            summaryLabel = "可总结",
            isRecording = false,
            actionState = RecordingActionState(
                canStart = false,
                canPause = false,
                canResume = true,
                switchingFromAnotherSession = false
            ),
            canRename = true,
            canFinish = true,
            canStartSummary = true,
            startSummaryDisabledReason = null
        )
    }

    private fun qaMessage(
        id: String,
        question: String,
        answer: String?,
        status: QaMessageStatus,
        errorMessage: String? = null
    ): SessionQaMessage {
        return SessionQaMessage(
            id = id,
            question = question,
            answer = answer,
            askedAt = 1_000L,
            answeredAt = 2_000L,
            status = status,
            modelName = "deepseek-chat",
            errorMessage = errorMessage
        )
    }
}
