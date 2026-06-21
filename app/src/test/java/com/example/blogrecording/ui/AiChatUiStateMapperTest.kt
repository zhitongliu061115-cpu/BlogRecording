package com.example.blogrecording.ui

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
    fun selectPodcastStartsScopedAssistantConversation() {
        val synced = AiChatUiStateMapper.syncFromHome(
            current = AiChatUiState(),
            home = HomeUiState(cards = listOf(card()), isEmpty = false)
        )

        val selected = AiChatUiStateMapper.selectPodcast(synced, "session-1", nowMillis = 1_000L)

        assertEquals("session-1", selected.selectedSessionId)
        assertFalse(selected.isChoosingPodcast)
        assertEquals(AiChatSender.ASSISTANT, selected.messages.single().sender)
        assertTrue(selected.messages.single().text.contains("Episode A"))
    }

    @Test
    fun startNewConversationClearsSelectedPodcastAndMessages() {
        val selected = AiChatUiState(
            selectedSessionId = "session-1",
            isChoosingPodcast = false,
            messages = listOf(
                com.example.blogrecording.ui.state.AiChatMessageUiState(
                    id = "message",
                    text = "hello",
                    sender = AiChatSender.USER,
                    timestampLabel = "10:00"
                )
            ),
            draftQuestion = "question"
        )

        val reset = AiChatUiStateMapper.startNewConversation(selected)

        assertEquals(null, reset.selectedSessionId)
        assertTrue(reset.isChoosingPodcast)
        assertTrue(reset.messages.isEmpty())
        assertEquals("", reset.draftQuestion)
    }

    @Test
    fun sendDraftAppendsUserAndAssistantMessages() {
        val synced = AiChatUiStateMapper.syncFromHome(
            current = AiChatUiState(
                draftQuestion = "  本期重点是什么？  "
            ),
            home = HomeUiState(cards = listOf(card()), isEmpty = false)
        )
        val selected = AiChatUiStateMapper.selectPodcast(
            current = synced,
            sessionId = "session-1",
            nowMillis = 1_000L
        )
        val withDraft = AiChatUiStateMapper.updateDraft(selected, "  本期重点是什么？  ")

        val sent = AiChatUiStateMapper.sendDraft(withDraft, nowMillis = 2_000L)

        assertEquals("", sent.draftQuestion)
        assertEquals(3, sent.messages.size)
        assertEquals(AiChatSender.USER, sent.messages[1].sender)
        assertEquals("本期重点是什么？", sent.messages[1].text)
        assertEquals(AiChatSender.ASSISTANT, sent.messages[2].sender)
        assertTrue(sent.messages[2].text.contains("Episode A"))
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
}
