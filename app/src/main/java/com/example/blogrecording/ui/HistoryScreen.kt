package com.example.blogrecording.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.ui.state.AppUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: AppUiState,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("历史", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text("返回") }
        }
        if (state.sessions.isEmpty()) {
            Text("暂无记录。")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions) { session ->
                    SessionCard(session = session, onClick = { onOpenDetail(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: RecordingSessionEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium)
            Text("创建时间：${formatTime(session.createdAt)}")
            Text("音频来源：${session.sourceType.toLabel()} · 字数：${session.transcript.length} · 片段：${session.segmentCount} · 说话人：${session.detectedSpeakerCount}")
            Text("总结：${if (session.summary.isNullOrBlank()) "未生成" else "已生成"} · ASR：${session.asrModelName} · 摘要模型：${session.summaryModelName}")
        }
    }
}

private fun AudioSourceType.toLabel(): String = when (this) {
    AudioSourceType.INTERNAL_AUDIO -> "系统内录"
    AudioSourceType.MICROPHONE -> "麦克风"
}

private fun formatTime(timeMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeMs))
}
