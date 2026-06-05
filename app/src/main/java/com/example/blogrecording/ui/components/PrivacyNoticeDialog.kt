package com.example.blogrecording.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PrivacyNoticeDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("隐私与费用风险") },
        text = {
            Text(
                "本 App 不自建后端。音频、VAD、ASR 和说话人分离只在本机处理；只会把转写文本、时间戳和说话人标签发送给 DeepSeek 生成总结。DeepSeek API Key 保存在本机并由本机直接请求 DeepSeek，Key 泄露和调用费用风险由用户自行承担。"
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("我已了解并继续")
            }
        }
    )
}
