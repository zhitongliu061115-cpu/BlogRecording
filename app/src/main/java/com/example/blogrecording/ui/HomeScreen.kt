package com.example.blogrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.toTranscriptText
import com.example.blogrecording.ui.components.ModelStatusPanel
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState

@Composable
fun HomeScreen(
    state: AppUiState,
    onStartInternal: () -> Unit,
    onStartMic: () -> Unit,
    onStop: () -> Unit,
    onGenerateSummary: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Podcast Recap Local ASR", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前状态：${state.recordingStatus.toLabel()}")
                Text("音频来源：${state.audioSourceType.toLabel()}")
                Text("转写状态：${state.vadLabel}")
                state.error?.let {
                    Text("错误：${it.toUserMessage()}", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartInternal) { Text("开始内录") }
            Button(onClick = onStartMic) { Text("开始麦克风录音") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStop) { Text("停止") }
            OutlinedButton(onClick = onGenerateSummary, enabled = state.currentSession?.transcript?.isNotBlank() == true && !state.isGeneratingSummary) {
                Text(if (state.isGeneratingSummary) "总结中" else "生成总结")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNavigate(AppScreen.HISTORY) }) { Text("查看历史") }
            OutlinedButton(onClick = { onNavigate(AppScreen.SETTINGS) }) { Text("设置") }
        }

        ModelStatusPanel(state.modelStatus)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("分片异步转写", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val transcript = if (state.currentSegments.isNotEmpty()) {
                    state.currentSegments.joinToString("\n\n") { it.toTranscriptText() }
                } else {
                    state.currentSession?.transcript.orEmpty()
                }
                Text(transcript.ifBlank { "暂无转写。模型已内置，录音会按设置时长分片后异步写入文本。" })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("隐私边界", style = MaterialTheme.typography.titleMedium)
                Text("不上传原始音频、PCM、音频片段、声纹向量或 speaker embedding。")
                Text("仅将转写文本、时间戳和说话人标签发送给 DeepSeek。")
                Text("如果系统或被录 App 不允许内录，请切换麦克风模式。")
            }
        }
    }
}

private fun RecordingStatus.toLabel(): String {
    return when (this) {
        RecordingStatus.NOT_STARTED -> "未开始"
        RecordingStatus.CAPTURING_AUDIO -> "正在捕获音频"
        RecordingStatus.VAD_DETECTING -> "正在 VAD 检测"
        RecordingStatus.DIARIZING -> "正在说话人分离"
        RecordingStatus.TRANSCRIBING -> "正在转写"
        RecordingStatus.SUMMARIZING -> "正在总结"
        RecordingStatus.COMPLETED -> "已完成"
        RecordingStatus.ERROR -> "出错"
    }
}

private fun AudioSourceType?.toLabel(): String {
    return when (this) {
        AudioSourceType.INTERNAL_AUDIO -> "系统内录"
        AudioSourceType.MICROPHONE -> "麦克风"
        null -> "未选择"
    }
}
