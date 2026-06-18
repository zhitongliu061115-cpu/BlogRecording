package com.example.blogrecording.summary

import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SummaryStatus

data class SessionSummaryEligibility(
    val status: SummaryStatus,
    val canStart: Boolean,
    val disabledReason: String?
)

object SessionSummaryEligibilityPolicy {
    const val MISSING_TRANSCRIPT_MESSAGE = "Transcript text is required"
    const val ACTIVE_RECORDING_MESSAGE = "Pause recording before summary"
    const val SUMMARY_IN_PROGRESS_MESSAGE = "Summary is already running"

    fun evaluate(
        detail: PodcastSessionDetail,
        aggregateTranscript: String = SessionTranscriptAggregator.aggregate(detail)
    ): SessionSummaryEligibility {
        val session = detail.session
        if (
            session.status == PodcastSessionStatus.SUMMARIZING ||
            session.summary?.status == SummaryStatus.SUMMARIZING
        ) {
            return SessionSummaryEligibility(
                status = SummaryStatus.SUMMARIZING,
                canStart = false,
                disabledReason = SUMMARY_IN_PROGRESS_MESSAGE
            )
        }

        if (
            session.status == PodcastSessionStatus.RECORDING ||
            session.activeSegmentId != null ||
            detail.recordingSegments.any { it.status == RecordingSegmentStatus.RECORDING }
        ) {
            return SessionSummaryEligibility(
                status = SummaryStatus.NOT_READY,
                canStart = false,
                disabledReason = ACTIVE_RECORDING_MESSAGE
            )
        }

        if (aggregateTranscript.isBlank()) {
            return SessionSummaryEligibility(
                status = SummaryStatus.NOT_READY,
                canStart = false,
                disabledReason = MISSING_TRANSCRIPT_MESSAGE
            )
        }

        val status = when (session.summary?.status) {
            SummaryStatus.FAILED -> SummaryStatus.FAILED
            SummaryStatus.SUMMARIZED -> SummaryStatus.SUMMARIZED
            else -> SummaryStatus.READY
        }
        return SessionSummaryEligibility(
            status = status,
            canStart = true,
            disabledReason = null
        )
    }
}
