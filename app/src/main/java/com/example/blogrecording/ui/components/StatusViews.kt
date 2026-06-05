package com.example.blogrecording.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus

@Composable
fun ModelStatusPanel(status: ModelStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("模型状态", style = MaterialTheme.typography.titleMedium)
        StatusChip("SenseVoice", status.senseVoice)
        StatusChip("VAD", status.vad)
        StatusChip("说话人", status.diarization)
    }
}

@Composable
private fun StatusChip(label: String, status: ModelLoadStatus) {
    val text = when (status) {
        ModelLoadStatus.LOADED -> "$label：已加载"
        ModelLoadStatus.MISSING -> "$label：未找到"
        ModelLoadStatus.INIT_FAILED -> "$label：初始化失败"
    }
    AssistChip(onClick = {}, label = { Text(text) })
}
