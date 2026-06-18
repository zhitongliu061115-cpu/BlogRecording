package com.example.blogrecording.ui

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.ui.state.HomeUiState
import com.example.blogrecording.ui.state.PodcastCardUiState
import com.example.blogrecording.ui.state.RecordingActionState
import com.example.blogrecording.ui.state.RenameDialogUiState

object HomeUiStateMapper {
    fun map(
        details: List<PodcastSessionDetail>,
        renameDialog: RenameDialogUiState? = null,
        error: AppError? = null
    ): HomeUiState {
        val activeRecordingSessionId = details
            .firstOrNull { detail ->
                detail.session.status == PodcastSessionStatus.RECORDING &&
                    detail.session.activeSegmentId != null
            }
            ?.session
            ?.id

        return HomeUiState(
            cards = details.map { detail ->
                detail.toCard(activeRecordingSessionId)
            },
            isEmpty = details.isEmpty(),
            activeRecordingSessionId = activeRecordingSessionId,
            renameDialog = renameDialog,
            errorMessage = error?.toUserMessage()
        )
    }

    private fun PodcastSessionDetail.toCard(
        activeRecordingSessionId: String?
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
        val isAnotherSessionRecording = activeRecordingSessionId != null && !isRecording
        val canStart = !isRecording && session.status == PodcastSessionStatus.DRAFT
        val canResume = !isRecording && session.status in RESUMABLE_STATUSES
        val canPause = isRecording
        val canFinish = when {
            isRecording -> true
            session.status == PodcastSessionStatus.PAUSED && hasCompletedSegment -> true
            session.status == PodcastSessionStatus.PROCESSING && hasAnySegment -> true
            session.status == PodcastSessionStatus.ERROR && hasCompletedSegment -> true
            else -> false
        }
        val canStartSummary = hasTranscript &&
            activeRecordingSessionId == null &&
            session.status in SUMMARY_STARTABLE_STATUSES

        return PodcastCardUiState(
            sessionId = session.id,
            title = session.title,
            statusLabel = session.status.toStatusLabel(isRecording),
            durationLabel = totalDurationMs.toDurationLabel(),
            segmentCountLabel = "${recordingSegments.size} 段",
            transcriptionLabel = transcriptionLabel(session),
            summaryLabel = summaryLabel(session),
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
                session = session
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

    private fun summaryLabel(session: PodcastSession): String {
        val status = session.summary?.status ?: if (session.transcript.isBlank()) {
            SummaryStatus.NOT_READY
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
        session: PodcastSession
    ): String? {
        return when {
            !hasTranscript -> "没有可总结的转写"
            activeRecordingSessionId != null || isRecording -> "请先暂停当前录音"
            session.status !in SUMMARY_STARTABLE_STATUSES -> "当前状态不可总结"
            else -> null
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
