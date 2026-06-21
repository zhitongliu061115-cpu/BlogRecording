package com.example.blogrecording.ui.state

data class AiChatUiState(
    val selectedSessionId: String? = null,
    val isChoosingPodcast: Boolean = true,
    val cards: List<AiPodcastCardUiState> = emptyList(),
    val messages: List<AiChatMessageUiState> = emptyList(),
    val draftQuestion: String = "",
    val isAsking: Boolean = false
)

data class AiPodcastCardUiState(
    val sessionId: String,
    val title: String,
    val statusLabel: String,
    val durationLabel: String,
    val transcriptionLabel: String,
    val summaryLabel: String,
    val tagLabels: List<String>,
    val transcriptPreview: String
)

data class AiChatMessageUiState(
    val id: String,
    val text: String,
    val sender: AiChatSender,
    val timestampLabel: String,
    val statusLabel: String? = null,
    val retryMessageId: String? = null,
    val isError: Boolean = false
)

enum class AiChatSender {
    USER,
    ASSISTANT
}
