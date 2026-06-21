package com.example.blogrecording.ui

import com.example.blogrecording.ui.state.AiChatMessageUiState
import com.example.blogrecording.ui.state.AiChatSender
import com.example.blogrecording.ui.state.AiChatUiState
import com.example.blogrecording.ui.state.AiPodcastCardUiState
import com.example.blogrecording.ui.state.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiChatUiStateMapper {
    fun syncFromHome(
        current: AiChatUiState,
        home: HomeUiState
    ): AiChatUiState {
        val cards = home.cards.map { card ->
            AiPodcastCardUiState(
                sessionId = card.sessionId,
                title = card.title,
                statusLabel = card.statusLabel,
                durationLabel = card.durationLabel,
                transcriptionLabel = card.transcriptionLabel,
                summaryLabel = card.summaryLabel,
                tagLabels = card.tagLabels,
                transcriptPreview = card.transcriptPreviewSnippets
                    .joinToString(separator = "\n") { it.text }
                    .ifBlank { "暂无转写内容" }
            )
        }
        val selectedStillExists = current.selectedSessionId != null &&
            cards.any { it.sessionId == current.selectedSessionId }
        return current.copy(
            selectedSessionId = current.selectedSessionId.takeIf { selectedStillExists },
            isChoosingPodcast = current.isChoosingPodcast || !selectedStillExists,
            cards = cards
        )
    }

    fun selectPodcast(
        current: AiChatUiState,
        sessionId: String,
        nowMillis: Long
    ): AiChatUiState {
        val card = current.cards.firstOrNull { it.sessionId == sessionId } ?: return current
        return current.copy(
            selectedSessionId = sessionId,
            isChoosingPodcast = false,
            draftQuestion = "",
            messages = listOf(
                AiChatMessageUiState(
                    id = "assistant-$nowMillis",
                    text = "我会围绕《${card.title}》回答问题。你可以问我这期的重点、时间线或可执行事项。",
                    sender = AiChatSender.ASSISTANT,
                    timestampLabel = formatTime(nowMillis)
                )
            )
        )
    }

    fun startNewConversation(current: AiChatUiState): AiChatUiState {
        return current.copy(
            selectedSessionId = null,
            isChoosingPodcast = true,
            draftQuestion = "",
            messages = emptyList()
        )
    }

    fun updateDraft(
        current: AiChatUiState,
        draft: String
    ): AiChatUiState {
        return current.copy(draftQuestion = draft)
    }

    fun sendDraft(
        current: AiChatUiState,
        nowMillis: Long
    ): AiChatUiState {
        val question = current.draftQuestion.trim()
        val selectedTitle = current.cards.firstOrNull {
            it.sessionId == current.selectedSessionId
        }?.title ?: return current
        if (question.isBlank()) return current

        return current.copy(
            draftQuestion = "",
            messages = current.messages + listOf(
                AiChatMessageUiState(
                    id = "user-$nowMillis",
                    text = question,
                    sender = AiChatSender.USER,
                    timestampLabel = formatTime(nowMillis)
                ),
                AiChatMessageUiState(
                    id = "assistant-${nowMillis + 1}",
                    text = "已收到。我会基于《${selectedTitle}》的转写和总结来回答：$question",
                    sender = AiChatSender.ASSISTANT,
                    timestampLabel = formatTime(nowMillis)
                )
            )
        )
    }

    private fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
    }
}
