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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.example.blogrecording.ui.components.AppSpacing
import com.example.blogrecording.ui.components.ModelStatusPanel
import com.example.blogrecording.ui.components.PodcastTopBar
import com.example.blogrecording.ui.components.SettingSection
import com.example.blogrecording.ui.components.StatusPill
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
            .padding(horizontal = AppSpacing.page, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.section)
    ) {
        PodcastTopBar(
            title = "设置",
            subtitle = "调整 AI 与本地处理参数",
            onBack = onBack
        )

        SettingSection(
            title = "DeepSeek",
            description = "Key 使用 Android Keystore 加密并仅保存在本机。"
        ) {
            StatusPill(
                text = if (state.hasApiKey) "API Key 已配置" else "API Key 未配置",
                icon = Icons.Rounded.Key,
                containerColor = if (state.hasApiKey) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                contentColor = if (state.hasApiKey) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("DeepSeek API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSaveApiKey(apiKey)
                        apiKey = ""
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text("保存 Key", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onDeleteApiKey, enabled = state.hasApiKey) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text("删除 Key", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        SectionDivider()

        SettingSection(
            title = "总结偏好",
            description = "这些设置会用于新生成的总结。"
        ) {
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
                values = SummaryLanguage.entries,
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
                values = SummaryStyle.entries,
                valueLabel = { it.displayName },
                onSelect = { draft = draft.copy(summaryStyle = it) }
            )
        }

        SectionDivider()

        SettingSection(
            title = "本地语音处理",
            description = "高级参数会影响切片、速度和说话人识别效果。"
        ) {
            SwitchRow(
                label = "启用 VAD",
                supportingText = "过滤静音并保留有语音的片段",
                checked = draft.enableVad,
                onChange = { draft = draft.copy(enableVad = it) }
            )
            SwitchRow(
                label = "启用说话人分离",
                supportingText = "为不同说话人分配本地标签",
                checked = draft.enableSpeakerDiarization,
                onChange = { draft = draft.copy(enableSpeakerDiarization = it) }
            )
            Text("最大说话人数  ${draft.maxSpeakerCount}", style = MaterialTheme.typography.titleSmall)
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
                transcriptionChunkDurationText = it.filter(Char::isDigit)
            }
        }

        SectionDivider()

        SettingSection(
            title = "模型状态",
            description = "SenseVoice、VAD 和说话人模型随安装包提供。"
        ) {
            ModelStatusPanel(state.modelStatus)
            Text(
                "模型随安装包内置，首次启动会自动复制到 App 私有目录，用户不需要手动填写模型路径。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "当前状态：${if (state.settings.sherpaModelRootPath.isBlank()) "初始化中" else "已自动安装到本机私有目录"}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        SectionDivider()

        SettingSection(
            title = "隐私",
            description = "原始音频、声纹向量和 speaker embedding 不会上传。"
        ) {
            Text(
                "使用 AI 总结或问答时，仅发送转写文本、时间戳和说话人标签到 DeepSeek。",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                val chunkDurationMs = transcriptionChunkDurationText.toLongOrNull()
                    ?.coerceIn(10_000L, 600_000L)
                    ?: draft.transcriptionChunkDurationMs
                onSaveSettings(draft.copy(transcriptionChunkDurationMs = chunkDurationMs))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Text("保存设置", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
private fun SwitchRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
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
