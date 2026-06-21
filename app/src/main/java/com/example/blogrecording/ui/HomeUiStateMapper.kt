package com.example.blogrecording.ui

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.summary.SessionSummaryEligibilityPolicy
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import com.example.blogrecording.ui.state.RecordingActionState
import com.example.blogrecording.ui.state.RenameDialogUiState
import com.example.blogrecording.ui.state.TranscriptPreviewSnippet

object HomeUiStateMapper {
    fun map(
        details: List<PodcastSessionDetail>,
        renameDialog: RenameDialogUiState? = null,
        error: AppError? = null,
        processingStage: ProcessingStageUiState = ProcessingStageUiState.idle(),
        processingSessionId: String? = null,
        hasApiKey: Boolean = true
    ): HomeUiState {
        val activeRecordingSessionId = details
            .firstOrNull { detail ->
                detail.session.status == PodcastSessionStatus.RECORDING &&
                    detail.session.activeSegmentId != null
            }
            ?.session
            ?.id

        val visibleDetails = details
            .sortedByDescending { it.session.updatedAt }
            .take(MAX_HOME_CARDS)

        return HomeUiState(
            cards = visibleDetails.map { detail ->
                detail.toCard(
                    activeRecordingSessionId = activeRecordingSessionId,
                    processingStage = processingStage,
                    processingSessionId = processingSessionId,
                    hasApiKey = hasApiKey
                )
            },
            isEmpty = details.isEmpty(),
            activeRecordingSessionId = activeRecordingSessionId,
            processingStage = processingStage,
            renameDialog = renameDialog,
            errorMessage = error?.toUserMessage()
        )
    }

    private fun PodcastSessionDetail.toCard(
        activeRecordingSessionId: String?,
        processingStage: ProcessingStageUiState,
        processingSessionId: String?,
        hasApiKey: Boolean
    ): PodcastCardUiState {
        val session = session
        val isRecording = session.id == activeRecordingSessionId
        val totalDurationMs = recordingSegments.sumOf { it.durationMs }
        val hasCompletedSegment = recordingSegments.any {
            it.status == RecordingSegmentStatus.COMPLETED ||
                it.status == RecordingSegmentStatus.PAUSED
        }
        val hasAnySegment = recordingSegments.isNotEmpty()
        val hasTranscript = session.transcript.isNotBlank() || transcriptSegments.any { it.text.isNotBlank() }
        val summaryEligibility = SessionSummaryEligibilityPolicy.evaluate(this)
        val isAnotherSessionRecording = activeRecordingSessionId != null && !isRecording
        val isImportedSession = session.sourceType == AudioSourceType.LOCAL_MEDIA
        val canStart = !isImportedSession && !isRecording && session.status == PodcastSessionStatus.DRAFT
        val canResume = !isImportedSession && !isRecording && session.status in RESUMABLE_STATUSES
        val canPause = !isImportedSession && isRecording
        val canFinish = when {
            isImportedSession -> false
            isRecording -> true
            session.status == PodcastSessionStatus.PAUSED && hasCompletedSegment -> true
            session.status == PodcastSessionStatus.PROCESSING && hasAnySegment -> true
            session.status == PodcastSessionStatus.ERROR && hasCompletedSegment -> true
            else -> false
        }
        val canStartSummary = summaryEligibility.canStart &&
            activeRecordingSessionId == null &&
            hasApiKey &&
            session.status in SUMMARY_STARTABLE_STATUSES

        return PodcastCardUiState(
            sessionId = session.id,
            title = session.title,
            statusLabel = session.status.toStatusLabel(isRecording),
            durationLabel = totalDurationMs.toDurationLabel(),
            segmentCountLabel = "${recordingSegments.size} 段",
            transcriptionLabel = transcriptionLabel(session),
            processingStage = processingStage.takeIf {
                session.id == processingSessionId || session.id == activeRecordingSessionId
            } ?: session.toProcessingStage(),
            transcriptPreviewSnippets = transcriptPreviewSnippets(),
            tagLabels = session.tagGeneration.tags
                .sortedBy { it.order }
                .take(MAX_HOME_TAGS)
                .map { it.text },
            summaryLabel = summaryLabel(session, hasTranscript),
            isRecording = isRecording,
            actionState = RecordingActionState(
                canStart = canStart,
                canPause = canPause,
                canResume = canResume,
                switchingFromAnotherSession = isAnotherSessionRecording && (canStart || canResume)
            ),
            canRename = session.status != PodcastSessionStatus.SUMMARIZING,
            canFinish = canFinish,
            canStartSummary = canStartSummary,
            startSummaryDisabledReason = summaryDisabledReason(
                hasTranscript = hasTranscript,
                activeRecordingSessionId = activeRecordingSessionId,
                isRecording = isRecording,
                session = session,
                hasApiKey = hasApiKey,
                summaryEligibilityReason = summaryEligibility.disabledReason
            )
        )
    }

    private fun PodcastSessionStatus.toStatusLabel(isRecording: Boolean): String {
        if (isRecording) return "录制中"
        return when (this) {
            PodcastSessionStatus.DRAFT -> "草稿"
            PodcastSessionStatus.RECORDING -> "录制中"
            PodcastSessionStatus.PAUSED -> "已暂停"
            PodcastSessionStatus.PROCESSING -> "转写处理中"
            PodcastSessionStatus.READY_FOR_SUMMARY -> "可总结"
            PodcastSessionStatus.SUMMARIZING -> "总结中"
            PodcastSessionStatus.SUMMARIZED -> "已总结"
            PodcastSessionStatus.ERROR -> "需处理"
        }
    }

    private fun PodcastSessionDetail.transcriptionLabel(session: PodcastSession): String {
        return when {
            session.status == PodcastSessionStatus.PROCESSING -> "转写中"
            session.transcriptSegmentCount > 0 -> "已转写 ${session.transcriptSegmentCount} 段"
            transcriptSegments.isNotEmpty() -> "已转写 ${transcriptSegments.size} 段"
            session.transcript.isNotBlank() -> "已转写"
            else -> "暂无转写"
        }
    }

    private fun PodcastSessionDetail.transcriptPreviewSnippets(): List<TranscriptPreviewSnippet> {
        return transcriptSegments
            .filter { it.text.isNotBlank() }
            .sortedBy { it.startMs }
            .takeLast(MAX_TRANSCRIPT_PREVIEW_SNIPPETS)
            .map { segment ->
                TranscriptPreviewSnippet(
                    timestampLabel = segment.startMs.toDurationLabel(),
                    text = segment.text.trim()
                )
            }
    }

    private fun summaryLabel(session: PodcastSession, hasTranscript: Boolean): String {
        val status = session.summary?.status ?: if (session.transcript.isBlank()) {
            if (hasTranscript) SummaryStatus.READY else SummaryStatus.NOT_READY
        } else {
            SummaryStatus.READY
        }
        return when (status) {
            SummaryStatus.NOT_READY -> "未就绪"
            SummaryStatus.READY -> "可总结"
            SummaryStatus.SUMMARIZING -> "总结中"
            SummaryStatus.SUMMARIZED -> "已总结"
            SummaryStatus.FAILED -> "总结失败"
        }
    }

    private fun summaryDisabledReason(
        hasTranscript: Boolean,
        activeRecordingSessionId: String?,
        isRecording: Boolean,
        session: PodcastSession,
        hasApiKey: Boolean,
        summaryEligibilityReason: String?
    ): String? {
        return when {
            !hasTranscript -> "没有可总结的转写"
            !hasApiKey -> "请先配置 DeepSeek API Key"
            activeRecordingSessionId != null || isRecording -> "请先暂停当前录音"
            session.status !in SUMMARY_STARTABLE_STATUSES -> "当前状态不可总结"
            summaryEligibilityReason != null -> summaryEligibilityReason
            else -> null
        }
    }

    private fun PodcastSession.toProcessingStage(): ProcessingStageUiState {
        return when (status) {
            PodcastSessionStatus.DRAFT -> ProcessingStageUiState.idle()
            PodcastSessionStatus.RECORDING -> ProcessingStageUiState.capturing("系统内录")
            PodcastSessionStatus.PAUSED -> ProcessingStageUiState.paused()
            PodcastSessionStatus.PROCESSING -> ProcessingStageUiState.transcribing(1)
            PodcastSessionStatus.READY_FOR_SUMMARY -> ProcessingStageUiState(
                title = "可生成总结",
                message = "转写已就绪"
            )
            PodcastSessionStatus.SUMMARIZING -> ProcessingStageUiState.summarizing()
            PodcastSessionStatus.SUMMARIZED -> ProcessingStageUiState(
                title = "已总结",
                message = "总结已生成"
            )
            PodcastSessionStatus.ERROR -> ProcessingStageUiState(
                stage = com.example.blogrecording.ui.state.ProcessingStage.ERROR,
                title = "需要处理",
                message = errorMessage ?: "录制或处理失败",
                isWarning = true
            )
        }
    }

    private fun Long.toDurationLabel(): String {
        val totalSeconds = (this / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private const val MAX_HOME_CARDS = 5
    private const val MAX_TRANSCRIPT_PREVIEW_SNIPPETS = 3
    private const val MAX_HOME_TAGS = 4

    private val RESUMABLE_STATUSES = setOf(
        PodcastSessionStatus.PAUSED,
        PodcastSessionStatus.READY_FOR_SUMMARY,
        PodcastSessionStatus.SUMMARIZED,
        PodcastSessionStatus.ERROR
    )

    private val SUMMARY_STARTABLE_STATUSES = setOf(
        PodcastSessionStatus.PAUSED,
        PodcastSessionStatus.READY_FOR_SUMMARY,
        PodcastSessionStatus.SUMMARIZED,
        PodcastSessionStatus.ERROR
    )
}
