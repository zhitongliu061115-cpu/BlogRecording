package com.example.blogrecording.ui

import com.example.blogrecording.data.QaMessageStatus
import com.example.blogrecording.data.SessionQaMessage
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
        sessionId: String
    ): AiChatUiState {
        current.cards.firstOrNull { it.sessionId == sessionId } ?: return current
        return current.copy(
            selectedSessionId = sessionId,
            isChoosingPodcast = false,
            draftQuestion = "",
            messages = emptyList(),
            isAsking = false
        )
    }

    fun startNewConversation(current: AiChatUiState): AiChatUiState {
        return current.copy(
            selectedSessionId = null,
            isChoosingPodcast = true,
            draftQuestion = "",
            messages = emptyList(),
            isAsking = false
        )
    }

    fun updateDraft(
        current: AiChatUiState,
        draft: String
    ): AiChatUiState {
        return current.copy(draftQuestion = draft)
    }

    fun sendDraft(current: AiChatUiState): AiChatUiState {
        val question = current.draftQuestion.trim()
        current.selectedSessionId ?: return current
        if (question.isBlank()) return current

        return current.copy(draftQuestion = "")
    }

    fun syncQaHistory(
        current: AiChatUiState,
        messages: List<SessionQaMessage>,
        isAsking: Boolean
    ): AiChatUiState {
        return current.copy(
            messages = messages.flatMap { it.toAiMessages() },
            isAsking = isAsking
        )
    }

    private fun SessionQaMessage.toAiMessages(): List<AiChatMessageUiState> {
        val questionBubble = AiChatMessageUiState(
            id = "question-$id",
            text = question,
            sender = AiChatSender.USER,
            timestampLabel = formatTime(askedAt)
        )
        val answerBubble = when (status) {
            QaMessageStatus.ANSWERING -> AiChatMessageUiState(
                id = "answer-$id",
                text = "回答中",
                sender = AiChatSender.ASSISTANT,
                timestampLabel = answeredAt?.let(::formatTime) ?: formatTime(askedAt),
                statusLabel = "DeepSeek 正在回答"
            )
            QaMessageStatus.ANSWERED -> AiChatMessageUiState(
                id = "answer-$id",
                text = answer.orEmpty(),
                sender = AiChatSender.ASSISTANT,
                timestampLabel = answeredAt?.let(::formatTime) ?: formatTime(askedAt)
            )
            QaMessageStatus.FAILED -> AiChatMessageUiState(
                id = "answer-$id",
                text = errorMessage.orEmpty().ifBlank { "问答失败" },
                sender = AiChatSender.ASSISTANT,
                timestampLabel = answeredAt?.let(::formatTime) ?: formatTime(askedAt),
                statusLabel = "可重试",
                retryMessageId = id,
                isError = true
            )
            QaMessageStatus.BLOCKED_MISSING_API_KEY -> AiChatMessageUiState(
                id = "answer-$id",
                text = "请先在设置里配置 DeepSeek API Key",
                sender = AiChatSender.ASSISTANT,
                timestampLabel = formatTime(askedAt),
                isError = true
            )
            QaMessageStatus.BLOCKED_EMPTY_CONTENT -> AiChatMessageUiState(
                id = "answer-$id",
                text = "需要先有转写、总结或高光内容",
                sender = AiChatSender.ASSISTANT,
                timestampLabel = formatTime(askedAt),
                isError = true
            )
        }
        return listOf(questionBubble, answerBubble)
    }

    private fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
    }
}
