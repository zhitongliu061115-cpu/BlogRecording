package com.example.blogrecording.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.ui.components.AppIconButton
import com.example.blogrecording.ui.components.AppSpacing
import com.example.blogrecording.ui.components.EmptyState
import com.example.blogrecording.ui.components.ModelStatusPanel
import com.example.blogrecording.ui.components.PodcastTopBar
import com.example.blogrecording.ui.components.SectionHeader
import com.example.blogrecording.ui.components.StatusPill
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import com.example.blogrecording.ui.state.RecordingActionState
import com.example.blogrecording.ui.state.RenameDialogUiState
import com.example.blogrecording.ui.state.SummaryStylePickerState
import com.example.blogrecording.ui.state.TranscriptPreviewSnippet

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
    onStartSummaryWithStyle: (String, SummaryStyle) -> Unit = { _, _ -> },
    onDismissStylePicker: () -> Unit = {},
    onOpenDetail: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val home = state.home
    var urlImportDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.page, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.section)
    ) {
        PodcastTopBar(
            title = "BlogRecording",
            subtitle = "本地录音、转写与复盘"
        ) {
            AppIconButton(
                icon = Icons.Rounded.History,
                label = "历史",
                onClick = { onNavigate(AppScreen.HISTORY) }
            )
            AppIconButton(
                icon = Icons.Rounded.Settings,
                label = "设置",
                onClick = { onNavigate(AppScreen.SETTINGS) }
            )
        }

        home.errorMessage?.let { ErrorBanner(it) }
        state.error?.let { ErrorBanner(it.toUserMessage()) }

        CaptureSourcePanel(
            onCreateSession = onCreateSession,
            onStartInternal = onStartInternal,
            onStartMicrophone = onStartMicrophone,
            onImportLocalMedia = onImportLocalMedia,
            onImportUrlMedia = { urlImportDialogOpen = true }
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            SectionHeader(
                title = "播客库",
                supportingText = if (home.isEmpty) "记录会保存在本机" else "${home.cards.size} 个会话"
            )
            if (home.isEmpty) {
                HomeEmptyState(onCreateSession = onCreateSession)
            } else {
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
        }

        LocalReadinessPanel(state.modelStatus)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("音频留在本机", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "原始音频、PCM 和声纹只在本机处理。仅在你使用 AI 功能时发送转写文本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    home.renameDialog?.let { dialog ->
        RenamePodcastDialog(dialog, onRenameSession, onDismissRename)
    }
    state.summaryStylePicker?.let { picker ->
        SummaryStylePickerDialog(picker, onStartSummaryWithStyle, onDismissStylePicker)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaptureSourcePanel(
    onCreateSession: () -> Unit,
    onStartInternal: () -> Unit,
    onStartMicrophone: () -> Unit,
    onImportLocalMedia: () -> Unit,
    onImportUrlMedia: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "开始记录",
            supportingText = "选择最适合当前内容的来源"
        )
        Button(
            onClick = onCreateSession,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新建播客")
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            SourceButton("系统内录", Icons.AutoMirrored.Rounded.ScreenShare, onStartInternal, Modifier.weight(1f))
            SourceButton("麦克风录音", Icons.Rounded.Mic, onStartMicrophone, Modifier.weight(1f))
            SourceButton("导入音视频", Icons.AutoMirrored.Rounded.InsertDriveFile, onImportLocalMedia, Modifier.weight(1f))
            SourceButton("导入 URL", Icons.Rounded.Link, onImportUrlMedia, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SourceButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
fun HomeEmptyState(
    onCreateSession: () -> Unit,
    onImportLocalMedia: () -> Unit = {},
    onImportUrlMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Rounded.Podcasts,
        title = "还没有播客",
        message = "新建一个播客会话，或从上方直接录音和导入内容。",
        modifier = modifier
    ) {
        OutlinedButton(onClick = onCreateSession) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新建")
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
    val accent = if (state.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (state.isRecording) 2.dp else 1.dp, accent),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusPill(
                        text = state.statusLabel,
                        icon = if (state.isRecording) Icons.Rounded.RadioButtonChecked else null,
                        containerColor = if (state.isRecording) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (state.isRecording) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
                IconButton(onClick = onRename, enabled = state.canRename) {
                    Icon(Icons.Rounded.Edit, contentDescription = "重命名")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetadataText(state.durationLabel)
                MetadataText(state.segmentCountLabel)
                MetadataText(state.transcriptionLabel)
                MetadataText("摘要 ${state.summaryLabel}")
            }

            TagRow(state.tagLabels)
            ProcessingStagePanel(state.processingStage)
            TranscriptPreview(state.transcriptPreviewSnippets)

            SessionActionRow(
                state = state,
                onStartRecording = onStartRecording,
                onStartMicrophoneRecording = onStartMicrophoneRecording,
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
                onResumeMicrophoneRecording = onResumeMicrophoneRecording,
                onFinishSession = onFinishSession,
                onStartSummary = onStartSummary
            )

            state.startSummaryDisabledReason?.takeIf { !state.canStartSummary }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionActionRow(
    state: PodcastCardUiState,
    onStartRecording: () -> Unit,
    onStartMicrophoneRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onResumeMicrophoneRecording: () -> Unit,
    onFinishSession: () -> Unit,
    onStartSummary: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            state.actionState.canPause -> {
                Button(onClick = onPauseRecording) {
                    Icon(Icons.Rounded.Pause, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("暂停")
                }
                OutlinedButton(onClick = onFinishSession, enabled = state.canFinish) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("完成")
                }
            }
            state.actionState.canResume -> {
                Button(onClick = onResumeRecording) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.actionState.switchingFromAnotherSession) "切换续录" else "续录")
                }
                OutlinedButton(onClick = onResumeMicrophoneRecording) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("麦克风续录")
                }
            }
            state.actionState.canStart -> {
                Button(onClick = onStartRecording) {
                    Icon(Icons.AutoMirrored.Rounded.ScreenShare, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.actionState.switchingFromAnotherSession) "切换录制" else "开始")
                }
                OutlinedButton(onClick = onStartMicrophoneRecording) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("麦克风开始")
                }
            }
        }
        if (state.canFinish && !state.actionState.canPause) {
            OutlinedButton(onClick = onFinishSession) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("完成")
            }
        }
        if (state.canStartSummary) {
            FilledTonalButton(onClick = onStartSummary) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("开始总结")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>) {
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            StatusPill(
                text = "#$tag",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProcessingStagePanel(stage: ProcessingStageUiState) {
    val container = when {
        stage.isWarning -> MaterialTheme.colorScheme.errorContainer
        stage.isActive -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        stage.isWarning -> MaterialTheme.colorScheme.onErrorContainer
        stage.isActive -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stage.title, style = MaterialTheme.typography.titleSmall)
                stage.progressLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            if (stage.isActive) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(stage.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TranscriptPreview(snippets: List<TranscriptPreviewSnippet>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("最近转写", style = MaterialTheme.typography.labelLarge)
        if (snippets.isEmpty()) {
            Text(
                "暂无转写内容",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            snippets.take(3).forEach { snippet ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        snippet.timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(44.dp)
                    )
                    Text(
                        snippet.text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LocalReadinessPanel(status: ModelStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "本地处理",
            supportingText = "模型仅在这台设备上运行"
        )
        ModelStatusPanel(status)
    }
}

@Composable
private fun RenamePodcastDialog(
    state: RenameDialogUiState,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(state.sessionId, state.initialTitle) { mutableStateOf(state.initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
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
            Button(onClick = { onConfirm(state.sessionId, title) }, enabled = title.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun UrlImportDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
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
                    "支持公开内容，不处理登录、Cookie 或私有链接。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(url) }, enabled = url.isNotBlank()) { Text("导入") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SummaryStylePickerDialog(
    state: SummaryStylePickerState,
    onSelect: (String, SummaryStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
        title = { Text("选择总结风格") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStyle.entries.forEach { style ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(state.sessionId, style) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(style.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun PodcastSessionCardPreview() {
    PodcastSessionCard(
        state = PodcastCardUiState(
            sessionId = "session-1",
            title = "端侧 AI 与个人知识管理",
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
            actionState = RecordingActionState(false, true, false, false),
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
