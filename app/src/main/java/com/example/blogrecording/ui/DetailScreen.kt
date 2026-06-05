package com.example.blogrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.blogrecording.data.toTranscriptText
import com.example.blogrecording.ui.state.AppUiState

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
                Text(session?.summary?.ifBlank { "暂无总结。" } ?: "暂无总结。")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("说话人说明", style = MaterialTheme.typography.titleMedium)
                Text("Speaker 1、Speaker 2 是自动分离标签，不代表真实身份。多人同时说话、背景音乐和麦克风外放录音会降低准确性。")
            }
        }
    }
}
