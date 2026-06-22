package com.example.blogrecording.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelStatusPanel(status: ModelStatus) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusChip("SenseVoice", status.senseVoice)
        StatusChip("VAD", status.vad)
        StatusChip("说话人", status.diarization)
    }
}

@Composable
private fun StatusChip(label: String, status: ModelLoadStatus) {
    val text = when (status) {
        ModelLoadStatus.LOADED -> "$label 已加载"
        ModelLoadStatus.MISSING -> "$label 未找到"
        ModelLoadStatus.INIT_FAILED -> "$label 初始化失败"
    }
    val icon = when (status) {
        ModelLoadStatus.LOADED -> Icons.Rounded.CheckCircle
        ModelLoadStatus.MISSING -> Icons.AutoMirrored.Rounded.Help
        ModelLoadStatus.INIT_FAILED -> Icons.Rounded.Error
    }
    val container = when (status) {
        ModelLoadStatus.LOADED -> MaterialTheme.colorScheme.secondaryContainer
        ModelLoadStatus.MISSING -> MaterialTheme.colorScheme.tertiaryContainer
        ModelLoadStatus.INIT_FAILED -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (status) {
        ModelLoadStatus.LOADED -> MaterialTheme.colorScheme.onSecondaryContainer
        ModelLoadStatus.MISSING -> MaterialTheme.colorScheme.onTertiaryContainer
        ModelLoadStatus.INIT_FAILED -> MaterialTheme.colorScheme.onErrorContainer
    }
    StatusPill(
        text = text,
        icon = icon,
        containerColor = container,
        contentColor = content
    )
}
