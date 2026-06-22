package com.example.blogrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.QaMessageStatus
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.SessionQaMessage
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.toTranscriptText
import com.example.blogrecording.export.SessionExportFormat
import com.example.blogrecording.ui.components.AppSpacing
import com.example.blogrecording.ui.components.PodcastTopBar
import com.example.blogrecording.ui.components.SectionHeader
import com.example.blogrecording.ui.components.StatusPill
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState

@Composable
fun DetailScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onGenerateSummary: () -> Unit,
    onToggleHighlightFavorite: (String) -> Unit,
    onSaveExport: (SessionExportFormat) -> Unit,
    onShareExport: (SessionExportFormat) -> Unit,
    onAskQuestion: (String) -> Unit,
    onRetryQuestion: (String) -> Unit,
    onDelete: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val session = state.currentSession
    val transcriptWithMeta = if (state.currentSegments.isNotEmpty()) {
        state.currentSegments.joinToString("\n\n") { it.toTranscriptText() }
    } else {
        session?.transcript.orEmpty()
    }
    val plainTranscript = state.currentSegments.joinToString("\n") { it.text }
        .ifBlank { session?.transcript.orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.page, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.section)
    ) {
        PodcastTopBar(
            title = session?.title ?: "记录详情",
            subtitle = if (state.currentSegments.isEmpty()) "播客复盘" else "${state.currentSegments.size} 段转写",
            onBack = onBack
        ) {
            IconButton(
                onClick = onGenerateSummary,
                enabled = !state.isGeneratingSummary && !session?.transcript.isNullOrBlank()
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = if (state.isGeneratingSummary) "总结中" else "重新生成总结")
            }
        }

        state.error?.let {
            ErrorBanner(it.toUserMessage())
        }
        DetailProcessingStage(state.processingStage)

        ContentSection(
            title = "AI 总结",
            supportingText = "先看结论，再深入原文",
            iconContent = {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            SummaryContent(
                summary = state.currentPodcastSummary,
                legacyText = session?.summary
            )
        }

        TagSection(state.currentTagLabels)
        HighlightSection(state.currentHighlights, onToggleHighlightFavorite)

        ContentSection(
            title = "转写文本",
            supportingText = if (transcriptWithMeta.isBlank()) "暂无内容" else "包含时间戳与说话人标签"
        ) {
            Text(
                transcriptWithMeta.ifBlank { "暂无转写。" },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        SessionQaSection(
            messages = state.currentQaMessages,
            isAsking = state.isAskingQa,
            hasApiKey = state.hasApiKey,
            hasContent = session?.transcript?.isNotBlank() == true ||
                state.currentSegments.any { it.text.isNotBlank() } ||
                state.currentPodcastSummary != null ||
                state.currentTagLabels.isNotEmpty() ||
                state.currentHighlights.isNotEmpty(),
            onAskQuestion = onAskQuestion,
            onRetryQuestion = onRetryQuestion
        )

        UtilitySection(
            onCopyPlain = { clipboard.setText(AnnotatedString(plainTranscript)) },
            onCopyWithLabels = { clipboard.setText(AnnotatedString(transcriptWithMeta)) },
            onCopySummary = { clipboard.setText(AnnotatedString(session?.summary.orEmpty())) },
            enabled = session != null,
            onSaveExport = onSaveExport,
            onShareExport = onShareExport
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("说话人说明", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Speaker 1、Speaker 2 是自动分离标签，不代表真实身份。背景音乐、多人同时说话和外放录音会降低准确性。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("删除", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ContentSection(
    title: String,
    supportingText: String? = null,
    iconContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconContent?.let {
                    it()
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    supportingText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun UtilitySection(
    onCopyPlain: () -> Unit,
    onCopyWithLabels: () -> Unit,
    onCopySummary: () -> Unit,
    enabled: Boolean,
    onSaveExport: (SessionExportFormat) -> Unit,
    onShareExport: (SessionExportFormat) -> Unit
) {
    ContentSection(
        title = "复制与导出",
        supportingText = "将本期内容带到其他工具"
    ) {
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onCopyPlain) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("复制纯文本")
            }
            FilledTonalButton(onClick = onCopyWithLabels) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("复制带标签转写")
            }
            FilledTonalButton(onClick = onCopySummary) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("复制总结")
            }
            ExportMenuButton(
                label = "保存导出",
                icon = Icons.Rounded.Download,
                enabled = enabled,
                onSelect = onSaveExport
            )
            ExportMenuButton(
                label = "分享导出",
                icon = Icons.Rounded.IosShare,
                enabled = enabled,
                onSelect = onShareExport
            )
        }
    }
}

@Composable
private fun ExportMenuButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onSelect: (SessionExportFormat) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(enabled = enabled, onClick = { expanded = true }) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SessionExportFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.displayLabel()) },
                    onClick = {
                        expanded = false
                        onSelect(format)
                    }
                )
            }
        }
    }
}

private fun SessionExportFormat.displayLabel(): String = when (this) {
    SessionExportFormat.MARKDOWN -> "Markdown"
    SessionExportFormat.TXT -> "TXT"
    SessionExportFormat.JSON -> "JSON"
}

@Composable
private fun SessionQaSection(
    messages: List<SessionQaMessage>,
    isAsking: Boolean,
    hasApiKey: Boolean,
    hasContent: Boolean,
    onAskQuestion: (String) -> Unit,
    onRetryQuestion: (String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val disabledReason = when {
        !hasApiKey -> "请先在设置里配置 DeepSeek API Key"
        !hasContent -> "需要先有转写、总结或高光内容"
        isAsking -> "正在回答"
        else -> null
    }
    ContentSection(
        title = "单期 AI 问答",
        supportingText = "答案只基于当前播客内容",
        iconContent = {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
    ) {
        messages.forEach { message ->
            QaMessageItem(message, onRetryQuestion)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            placeholder = { Text("提问关键观点、行动项或某段内容") },
            enabled = !isAsking
        )
        Button(
            enabled = question.isNotBlank() && disabledReason == null,
            onClick = {
                val readyQuestion = question
                question = ""
                onAskQuestion(readyQuestion)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isAsking) "回答中" else "发送")
        }
        disabledReason?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (!hasApiKey || !hasContent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QaMessageItem(
    message: SessionQaMessage,
    onRetryQuestion: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("问：${message.question}", style = MaterialTheme.typography.titleSmall)
        when (message.status) {
            QaMessageStatus.ANSWERING -> Text("答：回答中", color = MaterialTheme.colorScheme.onSurfaceVariant)
            QaMessageStatus.ANSWERED -> Text("答：${message.answer.orEmpty()}")
            QaMessageStatus.FAILED -> {
                Text(
                    "失败：${message.errorMessage.orEmpty().ifBlank { "问答失败" }}",
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = { onRetryQuestion(message.id) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("重试")
                }
            }
            QaMessageStatus.BLOCKED_MISSING_API_KEY -> Text("未配置 API Key", color = MaterialTheme.colorScheme.error)
            QaMessageStatus.BLOCKED_EMPTY_CONTENT -> Text("缺少可问答内容", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun HighlightSection(
    highlights: List<SessionHighlight>,
    onToggleFavorite: (String) -> Unit
) {
    if (highlights.isEmpty()) return
    ContentSection(
        title = "高光 / 金句",
        supportingText = "${highlights.size} 条候选",
        iconContent = {
            Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        }
    ) {
        highlights.forEachIndexed { index, highlight ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(highlight.text, style = MaterialTheme.typography.bodyLarge)
                    highlight.timeLabel()?.let {
                        StatusPill(
                            it,
                            icon = Icons.Rounded.Schedule,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onToggleFavorite(highlight.id) }) {
                    Icon(
                        if (highlight.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (highlight.isFavorite) "取消收藏" else "收藏",
                        tint = if (highlight.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (index != highlights.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(tags: List<String>) {
    if (tags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("关键词", supportingText = "快速定位本期主题")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag -> StatusPill("#$tag") }
        }
    }
}

@Composable
private fun SummaryContent(summary: SessionSummary?, legacyText: String?) {
    val structured = summary?.structured
    if (structured == null) {
        Text((summary?.text ?: legacyText).orEmpty().ifBlank { "暂无总结。" })
        return
    }
    StructuredSummaryContent(structured, summary.text)
}

@Composable
private fun StructuredSummaryContent(structured: StructuredSummary, fallbackText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            structured.overview.ifBlank { fallbackText.ifBlank { "暂无总结。" } },
            style = MaterialTheme.typography.bodyLarge
        )
        TimelineChapterSection(structured.timelineChapters)
        SummarySection("关键要点", structured.keyPoints)
        SummarySection("行动项", structured.actionItems)
        SummarySection("开放问题", structured.openQuestions)
        SummarySection("金句候选", structured.quoteCandidates)
    }
}

@Composable
private fun TimelineChapterSection(chapters: List<TimelineChapter>) {
    if (chapters.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("章节时间线", style = MaterialTheme.typography.titleSmall)
        chapters.forEach { chapter ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("${chapter.timeLabel()}  ${chapter.title}".trim(), fontWeight = FontWeight.SemiBold)
                    chapter.keyPoints.forEach { point ->
                        Text("• $point", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        items.forEach { item -> Text("• $item") }
    }
}

private fun TimelineChapter.timeLabel(): String {
    val start = startMs?.formatTimelineMs()
    val end = endMs?.formatTimelineMs()
    return when {
        start != null && end != null -> "$start-$end"
        start != null -> start
        else -> ""
    }
}

private fun Long.formatTimelineMs(): String {
    val totalSeconds = this / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun SessionHighlight.timeLabel(): String? {
    val start = sourceStartMs?.formatTimelineMs()
    val end = sourceEndMs?.formatTimelineMs()
    return when {
        start != null && end != null -> "$start-$end"
        start != null -> start
        else -> null
    }
}

@Composable
private fun DetailProcessingStage(stage: ProcessingStageUiState) {
    if (!stage.isActive && !stage.isWarning) return
    val container = if (stage.isWarning) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = if (stage.isWarning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (stage.isWarning) Icons.Rounded.Error else Icons.Rounded.MoreHoriz,
                contentDescription = null
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stage.title, style = MaterialTheme.typography.titleSmall)
                stage.progressLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                Text(stage.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            "错误：$message",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
