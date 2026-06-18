package com.example.blogrecording.summary

import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.data.SummaryStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryEligibilityPolicyTest {
    @Test
    fun readyWhenTranscriptExistsAndSessionIsNotRecording() {
        val result = SessionSummaryEligibilityPolicy.evaluate(
            detail(session = session(status = PodcastSessionStatus.READY_FOR_SUMMARY)),
            aggregateTranscript = "speaker text"
        )

        assertEquals(SummaryStatus.READY, result.status)
        assertTrue(result.canStart)
        assertNull(result.disabledReason)
    }

    @Test
    fun notReadyWhenTranscriptIsMissing() {
        val result = SessionSummaryEligibilityPolicy.evaluate(
            detail(session = session(status = PodcastSessionStatus.PAUSED)),
            aggregateTranscript = " "
        )

        assertEquals(SummaryStatus.NOT_READY, result.status)
        assertFalse(result.canStart)
        assertEquals(SessionSummaryEligibilityPolicy.MISSING_TRANSCRIPT_MESSAGE, result.disabledReason)
    }

    @Test
    fun activeRecordingBlocksFinalSummary() {
        val result = SessionSummaryEligibilityPolicy.evaluate(
            detail(
                session = session(
                    status = PodcastSessionStatus.RECORDING,
                    activeSegmentId = "segment-1"
                ),
                recordingSegments = listOf(
                    segment(id = "segment-1", status = RecordingSegmentStatus.RECORDING)
                )
            ),
            aggregateTranscript = "speaker text"
        )

        assertEquals(SummaryStatus.NOT_READY, result.status)
        assertFalse(result.canStart)
        assertEquals(SessionSummaryEligibilityPolicy.ACTIVE_RECORDING_MESSAGE, result.disabledReason)
    }

    @Test
    fun summarizingBlocksDuplicateStart() {
        val result = SessionSummaryEligibilityPolicy.evaluate(
            detail(
                session = session(
                    status = PodcastSessionStatus.SUMMARIZING,
                    summary = summary(SummaryStatus.SUMMARIZING)
                )
            ),
            aggregateTranscript = "speaker text"
        )

        assertEquals(SummaryStatus.SUMMARIZING, result.status)
        assertFalse(result.canStart)
        assertEquals(SessionSummaryEligibilityPolicy.SUMMARY_IN_PROGRESS_MESSAGE, result.disabledReason)
    }

    @Test
    fun failedAndSummarizedRemainStartableForRetry() {
        val failed = SessionSummaryEligibilityPolicy.evaluate(
            detail(session = session(summary = summary(SummaryStatus.FAILED))),
            aggregateTranscript = "speaker text"
        )
        val summarized = SessionSummaryEligibilityPolicy.evaluate(
            detail(session = session(summary = summary(SummaryStatus.SUMMARIZED))),
            aggregateTranscript = "speaker text"
        )

        assertEquals(SummaryStatus.FAILED, failed.status)
        assertTrue(failed.canStart)
        assertEquals(SummaryStatus.SUMMARIZED, summarized.status)
        assertTrue(summarized.canStart)
    }

    private fun detail(
        session: PodcastSession = session(),
        recordingSegments: List<RecordingSegment> = emptyList()
    ): PodcastSessionDetail {
        return PodcastSessionDetail(
            session = session,
            recordingSegments = recordingSegments,
            transcriptSegments = emptyList(),
            speakerProfiles = emptyList()
        )
    }

    private fun session(
        status: PodcastSessionStatus = PodcastSessionStatus.READY_FOR_SUMMARY,
        activeSegmentId: String? = null,
        summary: SessionSummary? = null
    ): PodcastSession {
        return PodcastSession(
            id = "session-1",
            title = "Episode",
            createdAt = 1L,
            updatedAt = 2L,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            activeSegmentId = activeSegmentId,
            lastCompletedSegmentId = null,
            transcript = "",
            summary = summary,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 0,
            recordingSegmentCount = 0,
            transcriptSegmentCount = 0,
            errorMessage = null,
            legacyRecordingSessionId = null
        )
    }

    private fun segment(
        id: String,
        status: RecordingSegmentStatus
    ): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = "session-1",
            index = 1,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            startedAt = 1L,
            endedAt = null,
            durationMs = 0L,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = null,
            channelCount = null,
            transcriptSegmentIds = emptyList(),
            errorMessage = null,
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun summary(status: SummaryStatus): SessionSummary {
        return SessionSummary(
            text = if (status == SummaryStatus.SUMMARIZED) "previous summary" else "",
            status = status,
            modelName = "deepseek-chat",
            generatedAt = null,
            updatedAt = 1L,
            errorMessage = null
        )
    }
}
