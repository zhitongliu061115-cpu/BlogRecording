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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.blogrecording.ui.components.AppSpacing
import com.example.blogrecording.ui.components.EmptyState
import com.example.blogrecording.ui.components.PodcastTopBar
import com.example.blogrecording.ui.components.SectionHeader
import com.example.blogrecording.ui.components.StatusPill
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
            .padding(horizontal = AppSpacing.page, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PodcastTopBar(
            title = "AI 助手",
            subtitle = if (state.isChoosingPodcast) "选择一期播客开始复盘" else "回答仅基于当前播客内容"
        ) {
            IconButton(
                onClick = onNewConversation,
                modifier = Modifier.testTag("ai-new-conversation")
            ) {
                Icon(Icons.Rounded.AddComment, contentDescription = "新对话")
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
            ChatInput(
                value = state.draftQuestion,
                enabled = state.selectedSessionId != null && !state.isAsking,
                isAsking = state.isAsking,
                onChange = onDraftChange,
                onSend = onSend
            )
        }
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
        SectionHeader(
            title = "选择播客",
            supportingText = "转写和总结会成为对话上下文"
        )
        if (cards.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Podcasts,
                title = "暂无可聊的播客",
                message = "先完成一次录音或导入，并等待本地转写完成。"
            ) {
                Text(
                    "播客准备好后会显示在这里",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cards) { card ->
                    AiPodcastCard(card, onClick = { onSelectPodcast(card.sessionId) })
                }
            }
        }
    }
}

@Composable
private fun AiPodcastCard(state: AiPodcastCardUiState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(286.dp)
            .height(218.dp)
            .testTag("ai-podcast-card-${state.sessionId}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(state.statusLabel)
                StatusPill(
                    state.durationLabel,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                state.transcriptPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (state.tagLabels.isNotEmpty()) {
                Text(
                    state.tagLabels.joinToString(prefix = "#", separator = "  #"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (messages.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "开始复盘",
                    message = "可以询问关键观点、行动项、分歧，或让 AI 解释某段内容。"
                ) {
                    Text(
                        "对话内容保存在当前播客中",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        items(messages) { message ->
            ChatBubble(message, onRetryQuestion)
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
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = "AI",
                    modifier = Modifier.padding(7.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.84f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    message.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                message.statusLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
                message.retryMessageId?.let { retryMessageId ->
                    FilledTonalButton(
                        onClick = { onRetryQuestion(retryMessageId) },
                        modifier = Modifier.testTag("ai-retry-$retryMessageId")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                enabled = enabled,
                placeholder = { Text(if (isAsking) "DeepSeek 回答中" else "围绕这期播客提问") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai-chat-input"),
                minLines = 1,
                maxLines = 4
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.testTag("ai-send")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = if (isAsking) "回答中" else "发送")
            }
        }
    }
}
