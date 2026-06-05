package com.example.blogrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.ui.components.ModelStatusPanel
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState

@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onDeleteApiKey: () -> Unit
) {
    var draft by remember(state.settings) { mutableStateOf(state.settings) }
    var apiKey by remember { mutableStateOf("") }
    var transcriptionChunkDurationText by remember(state.settings.transcriptionChunkDurationMs) {
        mutableStateOf(state.settings.transcriptionChunkDurationMs.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("DeepSeek API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSaveApiKey(apiKey); apiKey = "" }) { Text("保存 API Key") }
            OutlinedButton(onClick = onDeleteApiKey, enabled = state.hasApiKey) { Text("删除 API Key") }
        }
        Text(if (state.hasApiKey) "API Key 已加密保存到本机" else "API Key 未配置")

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draft.deepSeekModel,
            onValueChange = { draft = draft.copy(deepSeekModel = it) },
            label = { Text("DeepSeek 模型") },
            singleLine = true
        )
        EnumDropdown(
            label = "总结语言",
            value = draft.summaryLanguage,
            values = SummaryLanguage.values().toList(),
            valueLabel = {
                when (it) {
                    SummaryLanguage.CHINESE -> "中文"
                    SummaryLanguage.ENGLISH -> "英文"
                    SummaryLanguage.FOLLOW_PODCAST -> "跟随播客语言"
                }
            },
            onSelect = { draft = draft.copy(summaryLanguage = it) }
        )
        EnumDropdown(
            label = "总结风格",
            value = draft.summaryStyle,
            values = SummaryStyle.values().toList(),
            valueLabel = {
                when (it) {
                    SummaryStyle.BRIEF -> "简洁摘要"
                    SummaryStyle.DEEP_RECAP -> "深度复盘"
                    SummaryStyle.TIMELINE_NOTES -> "时间线笔记"
                    SummaryStyle.POINTS_QUOTES_ACTIONS -> "要点 + 金句 + 行动项"
                }
            },
            onSelect = { draft = draft.copy(summaryStyle = it) }
        )

        CheckboxRow("启用 VAD", draft.enableVad) { draft = draft.copy(enableVad = it) }
        CheckboxRow("启用说话人分离", draft.enableSpeakerDiarization) { draft = draft.copy(enableSpeakerDiarization = it) }
        Text("最大说话人数：${draft.maxSpeakerCount}")
        Slider(
            value = draft.maxSpeakerCount.toFloat(),
            onValueChange = { draft = draft.copy(maxSpeakerCount = it.toInt().coerceIn(2, 8)) },
            valueRange = 2f..8f,
            steps = 5
        )
        NumberField("VAD 阈值", draft.vadSpeechThreshold.toString()) {
            draft = draft.copy(vadSpeechThreshold = it.toFloatOrNull()?.coerceIn(0f, 1f) ?: draft.vadSpeechThreshold)
        }
        NumberField("最短语音片段 ms", draft.minSpeechDurationMs.toString()) {
            draft = draft.copy(minSpeechDurationMs = it.toLongOrNull() ?: draft.minSpeechDurationMs)
        }
        NumberField("最短静音 ms", draft.minSilenceDurationMs.toString()) {
            draft = draft.copy(minSilenceDurationMs = it.toLongOrNull() ?: draft.minSilenceDurationMs)
        }
        NumberField("最长语音片段 ms", draft.maxSpeechDurationMs.toString()) {
            draft = draft.copy(maxSpeechDurationMs = it.toLongOrNull() ?: draft.maxSpeechDurationMs)
        }
        NumberField("转写分片时长 ms", transcriptionChunkDurationText) {
            transcriptionChunkDurationText = it.filter { char -> char.isDigit() }
        }

        ModelStatusPanel(state.modelStatus)
        Text("模型随安装包内置，首次启动会自动复制到 App 私有目录，用户不需要手动填写模型路径。")
        Text("内置模型：SenseVoice、VAD、说话人分离。")
        Text("当前状态：${if (state.settings.sherpaModelRootPath.isBlank()) "初始化中" else "已自动安装到本机私有目录"}")
        Text("音频只在本地处理；不上传原始音频、声纹向量或 speaker embedding。")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val chunkDurationMs = transcriptionChunkDurationText.toLongOrNull()
                        ?.coerceIn(10_000L, 600_000L)
                        ?: draft.transcriptionChunkDurationMs
                    onSaveSettings(draft.copy(transcriptionChunkDurationMs = chunkDurationMs))
                }
            ) { Text("保存设置") }
            OutlinedButton(onClick = onBack) { Text("返回") }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    value: T,
    values: List<T>,
    valueLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            value = valueLabel(value),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach {
                DropdownMenuItem(
                    text = { Text(valueLabel(it)) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
