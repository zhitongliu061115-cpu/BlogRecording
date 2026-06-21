package com.example.blogrecording.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus
import com.example.blogrecording.ui.components.ModelStatusPanel
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import com.example.blogrecording.ui.state.RecordingActionState
import com.example.blogrecording.ui.state.RenameDialogUiState
import com.example.blogrecording.ui.state.TranscriptPreviewSnippet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: AppUiState,
    onCreateSession: () -> Unit,
    onImportLocalMedia: () -> Unit,
    onImportUrlMedia: (String) -> Unit,
    onStartInternal: () -> Unit,
    onStartMicrophone: () -> Unit,
    onStartInternalSession: (String) -> Unit,
    onStartMicrophoneSession: (String) -> Unit,
    onPauseRecording: (String) -> Unit,
    onResumeInternalSession: (String) -> Unit,
    onResumeMicrophoneSession: (String) -> Unit,
    onFinishSession: (String) -> Unit,
    onRequestRename: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDismissRename: () -> Unit,
    onStartSummary: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val home = state.home
    var urlImportDialogOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("BlogRecording", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(AppScreen.HISTORY) }) {
                    Text("历史")
                }
                OutlinedButton(onClick = { onNavigate(AppScreen.SETTINGS) }) {
                    Text("设置")
                }
            }
        }

        home.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.error?.let {
            Text(it.toUserMessage(), color = MaterialTheme.colorScheme.error)
        }

        if (home.isEmpty) {
            HomeEmptyState(
                onCreateSession = onCreateSession,
                onImportLocalMedia = onImportLocalMedia,
                onImportUrlMedia = { urlImportDialogOpen = true }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("播客", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCreateSession) {
                        Text("新建")
                    }
                    OutlinedButton(onClick = onImportLocalMedia) {
                        Text("导入音视频")
                    }
                    OutlinedButton(onClick = { urlImportDialogOpen = true }) {
                        Text("导入 URL")
                    }
                }
            }
            home.cards.forEach { card ->
                PodcastSessionCard(
                    state = card,
                    onStartRecording = { onStartInternalSession(card.sessionId) },
                    onStartMicrophoneRecording = { onStartMicrophoneSession(card.sessionId) },
                    onPauseRecording = { onPauseRecording(card.sessionId) },
                    onResumeRecording = { onResumeInternalSession(card.sessionId) },
                    onResumeMicrophoneRecording = { onResumeMicrophoneSession(card.sessionId) },
                    onFinishSession = { onFinishSession(card.sessionId) },
                    onRename = { onRequestRename(card.sessionId) },
                    onStartSummary = { onStartSummary(card.sessionId) },
                    onOpenDetail = { onOpenDetail(card.sessionId) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStartInternal) {
                Text("系统内录")
            }
            OutlinedButton(onClick = onStartMicrophone) {
                Text("麦克风录音")
            }
            OutlinedButton(onClick = onImportLocalMedia) {
                Text("导入音视频")
            }
            OutlinedButton(onClick = { urlImportDialogOpen = true }) {
                Text("导入 URL")
            }
        }

        ModelStatusPanel(state.modelStatus)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("隐私边界", style = MaterialTheme.typography.titleMedium)
                Text("原始音频、PCM、音频片段和说话人向量只在本机处理。")
                Text("总结仅发送转写文本、时间戳和说话人标签到 DeepSeek。")
            }
        }
    }

    home.renameDialog?.let { dialog ->
        RenamePodcastDialog(
            state = dialog,
            onConfirm = onRenameSession,
            onDismiss = onDismissRename
        )
    }
    if (urlImportDialogOpen) {
        UrlImportDialog(
            onConfirm = { url ->
                urlImportDialogOpen = false
                onImportUrlMedia(url)
            },
            onDismiss = { urlImportDialogOpen = false }
        )
    }
}

@Composable
fun HomeEmptyState(
    onCreateSession: () -> Unit,
    onImportLocalMedia: () -> Unit,
    onImportUrlMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("还没有播客", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCreateSession) {
                Text("新建播客")
            }
            OutlinedButton(onClick = onImportLocalMedia) {
                Text("导入音视频")
            }
            OutlinedButton(onClick = onImportUrlMedia) {
                Text("导入 URL")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PodcastSessionCard(
    state: PodcastCardUiState,
    onStartRecording: () -> Unit,
    onStartMicrophoneRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onResumeMicrophoneRecording: () -> Unit,
    onFinishSession: () -> Unit,
    onRename: () -> Unit,
    onStartSummary: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightColor = if (state.isRecording) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, highlightColor),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isRecording) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(state.statusLabel, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(
                    onClick = onRename,
                    enabled = state.canRename
                ) {
                    Text("重命名")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardInfoText("时长 ${state.durationLabel}")
                CardInfoText(state.segmentCountLabel)
                CardInfoText(state.transcriptionLabel)
                CardInfoText("摘要 ${state.summaryLabel}")
            }

            TagRow(tags = state.tagLabels)

            ProcessingStagePanel(state.processingStage)

            TranscriptPreview(snippets = state.transcriptPreviewSnippets)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartRecording,
                    enabled = state.actionState.canStart
                ) {
                    Text(if (state.actionState.switchingFromAnotherSession) "切换录制" else "开始")
                }
                OutlinedButton(
                    onClick = onStartMicrophoneRecording,
                    enabled = state.actionState.canStart
                ) {
                    Text("麦克风开始")
                }
                OutlinedButton(
                    onClick = onPauseRecording,
                    enabled = state.actionState.canPause
                ) {
                    Text("暂停")
                }
                OutlinedButton(
                    onClick = onResumeRecording,
                    enabled = state.actionState.canResume
                ) {
                    Text(if (state.actionState.switchingFromAnotherSession) "切换续录" else "续录")
                }
                OutlinedButton(
                    onClick = onResumeMicrophoneRecording,
                    enabled = state.actionState.canResume
                ) {
                    Text("麦克风续录")
                }
                OutlinedButton(
                    onClick = onFinishSession,
                    enabled = state.canFinish
                ) {
                    Text("完成")
                }
                OutlinedButton(
                    onClick = onStartSummary,
                    enabled = state.canStartSummary
                ) {
                    Text("开始总结")
                }
            }

            state.startSummaryDisabledReason?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>) {
    if (tags.isEmpty()) return
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

@Composable
private fun ProcessingStagePanel(stage: ProcessingStageUiState) {
    val container = when {
        stage.isWarning -> MaterialTheme.colorScheme.errorContainer
        stage.isActive -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                stage.progressLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(stage.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TranscriptPreview(snippets: List<TranscriptPreviewSnippet>) {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (snippets.isEmpty()) {
                Text("暂无转写内容", style = MaterialTheme.typography.bodySmall)
            } else {
                snippets.forEach { snippet ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = snippet.timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = snippet.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardInfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun RenamePodcastDialog(
    state: RenameDialogUiState,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(state.sessionId, state.initialTitle) {
        mutableStateOf(state.initialTitle)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名播客") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(state.sessionId, title) },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun UrlImportDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember {
        mutableStateOf("https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入 URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("小宇宙单期 / 直链媒体 / RSS") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "优先支持小宇宙单期链接；不支持登录、Cookie 或私有内容。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank()
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PodcastSessionCardPreview() {
    PodcastSessionCard(
        state = PodcastCardUiState(
            sessionId = "session-1",
            title = "Episode A",
            statusLabel = "录制中",
            durationLabel = "12:08",
            segmentCountLabel = "3 段",
            transcriptionLabel = "已转写 2 段",
            processingStage = ProcessingStageUiState.buffering(12_000L, 30_000L),
            transcriptPreviewSnippets = listOf(
                TranscriptPreviewSnippet("0:12", "欢迎来到本期播客。"),
                TranscriptPreviewSnippet("0:28", "我们先聊一下今天的主题。")
            ),
            tagLabels = listOf("AI", "播客"),
            summaryLabel = "未就绪",
            isRecording = true,
            actionState = RecordingActionState(
                canStart = false,
                canPause = true,
                canResume = false,
                switchingFromAnotherSession = false
            ),
            canRename = true,
            canFinish = true,
            canStartSummary = false,
            startSummaryDisabledReason = "请先暂停当前录音"
        ),
        onStartRecording = {},
        onStartMicrophoneRecording = {},
        onPauseRecording = {},
        onResumeRecording = {},
        onResumeMicrophoneRecording = {},
        onFinishSession = {},
        onRename = {},
        onStartSummary = {},
        onOpenDetail = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    HomeScreen(
        state = AppUiState(
            home = HomeUiState(),
            modelStatus = ModelStatus(
                senseVoice = ModelLoadStatus.LOADED,
                vad = ModelLoadStatus.LOADED,
                diarization = ModelLoadStatus.LOADED
            )
        ),
        onCreateSession = {},
        onImportLocalMedia = {},
        onImportUrlMedia = {},
        onStartInternal = {},
        onStartMicrophone = {},
        onStartInternalSession = {},
        onStartMicrophoneSession = {},
        onPauseRecording = {},
        onResumeInternalSession = {},
        onResumeMicrophoneSession = {},
        onFinishSession = {},
        onRequestRename = {},
        onRenameSession = { _, _ -> },
        onDismissRename = {},
        onStartSummary = {},
        onOpenDetail = {},
        onNavigate = {}
    )
}
