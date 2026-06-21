package com.example.blogrecording.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.blogrecording.ui.state.AiChatMessageUiState
import com.example.blogrecording.ui.state.AiChatSender
import com.example.blogrecording.ui.state.AiChatUiState
import com.example.blogrecording.ui.state.AiPodcastCardUiState

@Composable
fun AiChatScreen(
    state: AiChatUiState,
    onSelectPodcast: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetryQuestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI 助手", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(
                onClick = onNewConversation,
                modifier = Modifier.testTag("ai-new-conversation")
            ) {
                Text("新对话")
            }
        }

        if (state.isChoosingPodcast) {
            PodcastChooser(
                cards = state.cards,
                onSelectPodcast = onSelectPodcast,
                modifier = Modifier.weight(1f)
            )
        } else {
            ChatConversation(
                messages = state.messages,
                onRetryQuestion = onRetryQuestion,
                modifier = Modifier.weight(1f)
            )
        }

        ChatInput(
            value = state.draftQuestion,
            enabled = state.selectedSessionId != null && !state.isChoosingPodcast && !state.isAsking,
            isAsking = state.isAsking,
            onChange = onDraftChange,
            onSend = onSend
        )
    }
}

@Composable
private fun PodcastChooser(
    cards: List<AiPodcastCardUiState>,
    onSelectPodcast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("选择要聊的播客", style = MaterialTheme.typography.titleMedium)
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无可聊天的播客内容")
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cards) { card ->
                    AiPodcastCard(
                        state = card,
                        onClick = { onSelectPodcast(card.sessionId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPodcastCard(
    state: AiPodcastCardUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp)
            .testTag("ai-podcast-card-${state.sessionId}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text("${state.statusLabel} · ${state.durationLabel}", style = MaterialTheme.typography.bodySmall)
            Text(state.transcriptionLabel, style = MaterialTheme.typography.bodySmall)
            Text("总结 ${state.summaryLabel}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = state.transcriptPreview,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (state.tagLabels.isNotEmpty()) {
                Text(
                    text = state.tagLabels.joinToString(prefix = "#", separator = " #"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChatConversation(
    messages: List<AiChatMessageUiState>,
    onRetryQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages) { message ->
            ChatBubble(
                message = message,
                onRetryQuestion = onRetryQuestion
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: AiChatMessageUiState,
    onRetryQuestion: (String) -> Unit
) {
    val isUser = message.sender == AiChatSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(message.timestampLabel, style = MaterialTheme.typography.labelSmall)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 1.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier
                        .width(260.dp)
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            message.statusLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            message.retryMessageId?.let { retryMessageId ->
                OutlinedButton(
                    onClick = { onRetryQuestion(retryMessageId) },
                    modifier = Modifier.testTag("ai-retry-$retryMessageId")
                ) {
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    enabled: Boolean,
    isAsking: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            label = {
                Text(
                    when {
                        isAsking -> "DeepSeek 回答中"
                        enabled -> "输入问题"
                        else -> "请先选择播客"
                    }
                )
            },
            modifier = Modifier
                .weight(1f)
                .testTag("ai-chat-input"),
            singleLine = true
        )
        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.testTag("ai-send")
        ) {
            Text(if (isAsking) "回答中" else "发送")
        }
    }
}
