package com.example.blogrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.toTranscriptText
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState

@Composable
fun DetailScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onGenerateSummary: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val session = state.currentSession
    val transcriptWithMeta = if (state.currentSegments.isNotEmpty()) {
        state.currentSegments.joinToString("\n\n") { it.toTranscriptText() }
    } else {
        session?.transcript.orEmpty()
    }
    val plainTranscript = state.currentSegments.joinToString("\n") { it.text }.ifBlank { session?.transcript.orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(session?.title ?: "记录详情", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            Button(onClick = onGenerateSummary, enabled = !state.isGeneratingSummary && !session?.transcript.isNullOrBlank()) {
                Text(if (state.isGeneratingSummary) "总结中" else "重新生成总结")
            }
            OutlinedButton(onClick = onDelete) { Text("删除") }
        }
        state.error?.let {
            Text("错误：${it.toUserMessage()}", color = MaterialTheme.colorScheme.error)
        }
        DetailProcessingStage(state.processingStage)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(plainTranscript)) }) { Text("复制纯文本") }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(transcriptWithMeta)) }) { Text("复制带标签转写") }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(session?.summary.orEmpty())) }) { Text("复制总结") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("转写文本", style = MaterialTheme.typography.titleMedium)
                Text(transcriptWithMeta.ifBlank { "暂无转写。" })
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI 总结", style = MaterialTheme.typography.titleMedium)
                SummaryContent(
                    summary = state.currentPodcastSummary,
                    legacyText = session?.summary
                )
            }
        }
        TagSection(state.currentTagLabels)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("说话人说明", style = MaterialTheme.typography.titleMedium)
                Text("Speaker 1、Speaker 2 是自动分离标签，不代表真实身份。多人同时说话、背景音乐和麦克风外放录音会降低准确性。")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(tags: List<String>) {
    if (tags.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("关键词 / 标签", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: SessionSummary?,
    legacyText: String?
) {
    val structured = summary?.structured
    if (structured == null) {
        Text((summary?.text ?: legacyText).orEmpty().ifBlank { "暂无总结。" })
        return
    }
    StructuredSummaryContent(
        structured = structured,
        fallbackText = summary.text
    )
}

@Composable
private fun StructuredSummaryContent(
    structured: StructuredSummary,
    fallbackText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(structured.overview.ifBlank { fallbackText.ifBlank { "暂无总结。" } })
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("章节时间线", style = MaterialTheme.typography.titleSmall)
        chapters.forEach { chapter ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${chapter.timeLabel()} ${chapter.title}".trim())
                chapter.keyPoints.forEach { point ->
                    Text("- $point", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        items.forEach { item ->
            Text("- $item")
        }
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
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun DetailProcessingStage(stage: ProcessingStageUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stage.title, style = MaterialTheme.typography.titleMedium)
            stage.progressLabel?.let {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
            Text(stage.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
