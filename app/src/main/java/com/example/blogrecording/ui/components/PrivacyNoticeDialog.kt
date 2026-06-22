package com.example.blogrecording.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyNoticeDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = { Text("隐私与费用风险") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("音频、VAD、ASR 和说话人分离只在本机处理。")
                Text("使用 AI 功能时，仅发送转写文本、时间戳和说话人标签到 DeepSeek。")
                Text(
                    "API Key 加密保存在本机并由设备直接请求 DeepSeek，调用费用由你承担。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("我已了解并继续")
            }
        }
    )
}
