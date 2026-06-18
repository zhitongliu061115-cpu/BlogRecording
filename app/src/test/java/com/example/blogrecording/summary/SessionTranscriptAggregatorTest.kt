package com.example.blogrecording.summary

import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTranscriptAggregatorTest {
    @Test
    fun aggregatesByRecordingSegmentIndexThenTranscriptStartTime() {
        val detail = detail(
            recordingSegments = listOf(
                segment(id = "segment-2", index = 2),
                segment(id = "segment-1", index = 1)
            ),
            transcriptSegments = listOf(
                transcript(id = "t-3", recordingSegmentId = "segment-2", startMs = 200L, text = "third"),
                transcript(id = "t-2", recordingSegmentId = "segment-1", startMs = 200L, text = "second"),
                transcript(id = "t-1", recordingSegmentId = "segment-1", startMs = 100L, text = "first")
            )
        )

        val aggregate = SessionTranscriptAggregator.aggregate(detail)

        assertOrdered(aggregate, "first", "second", "third")
    }

    @Test
    fun preservesLegacyTranscriptSegmentsAfterLinkedSegments() {
        val detail = detail(
            recordingSegments = listOf(segment(id = "segment-1", index = 1)),
            transcriptSegments = listOf(
                transcript(id = "legacy-2", recordingSegmentId = null, startMs = 50L, text = "legacy later"),
                transcript(id = "linked", recordingSegmentId = "segment-1", startMs = 500L, text = "linked"),
                transcript(id = "legacy-1", recordingSegmentId = null, startMs = 10L, text = "legacy first")
            )
        )

        val aggregate = SessionTranscriptAggregator.aggregate(detail)

        assertOrdered(aggregate, "linked", "legacy first", "legacy later")
    }

    @Test
    fun keepsCompletedTranscriptWhenLaterSegmentIsBlankOrFailed() {
        val detail = detail(
            recordingSegments = listOf(
                segment(id = "segment-1", index = 1, status = RecordingSegmentStatus.COMPLETED),
                segment(id = "segment-2", index = 2, status = RecordingSegmentStatus.ERROR)
            ),
            transcriptSegments = listOf(
                transcript(id = "t-1", recordingSegmentId = "segment-1", startMs = 100L, text = "previous"),
                transcript(id = "t-blank", recordingSegmentId = "segment-2", startMs = 200L, text = " ")
            )
        )

        val aggregate = SessionTranscriptAggregator.aggregate(detail)

        assertTrue(aggregate.contains("previous"))
        assertFalse(aggregate.contains("t-blank"))
    }

    @Test
    fun ignoresTranscriptSegmentsFromOtherSessions() {
        val detail = detail(
            transcriptSegments = listOf(
                transcript(id = "own", sessionId = "session-1", text = "own transcript"),
                transcript(id = "other", sessionId = "session-2", text = "other transcript")
            )
        )

        val aggregate = SessionTranscriptAggregator.aggregate(detail)

        assertTrue(aggregate.contains("own transcript"))
        assertFalse(aggregate.contains("other transcript"))
    }

    private fun assertOrdered(text: String, vararg parts: String) {
        var previous = -1
        parts.forEach { part ->
            val index = text.indexOf(part)
            assertTrue("$part was not found in $text", index >= 0)
            assertTrue("$part was not ordered after previous part", index > previous)
            previous = index
        }
    }

    private fun detail(
        sessionId: String = "session-1",
        recordingSegments: List<RecordingSegment> = emptyList(),
        transcriptSegments: List<TranscriptSegmentEntity> = emptyList()
    ): PodcastSessionDetail {
        return PodcastSessionDetail(
            session = session(id = sessionId),
            recordingSegments = recordingSegments,
            transcriptSegments = transcriptSegments,
            speakerProfiles = emptyList()
        )
    }

    private fun session(id: String): PodcastSession {
        return PodcastSession(
            id = id,
            title = "Episode",
            createdAt = 1L,
            updatedAt = 1L,
            sourceType = AudioSourceType.MICROPHONE,
            status = PodcastSessionStatus.PAUSED,
            activeSegmentId = null,
            lastCompletedSegmentId = null,
            transcript = "",
            summary = null,
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
        index: Int,
        status: RecordingSegmentStatus = RecordingSegmentStatus.COMPLETED
    ): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = "session-1",
            index = index,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            startedAt = index * 1_000L,
            endedAt = index * 1_000L + 500L,
            durationMs = 500L,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = 16_000,
            channelCount = 1,
            transcriptSegmentIds = emptyList(),
            errorMessage = if (status == RecordingSegmentStatus.ERROR) "ASR failed" else null,
            createdAt = index * 1_000L,
            updatedAt = index * 1_000L + 500L
        )
    }

    private fun transcript(
        id: String,
        sessionId: String = "session-1",
        recordingSegmentId: String? = null,
        startMs: Long = 100L,
        text: String
    ): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = id,
            sessionId = sessionId,
            recordingSegmentId = recordingSegmentId,
            startMs = startMs,
            endMs = startMs + 500L,
            speakerId = "speaker-1",
            speakerDisplayName = "Speaker 1",
            text = text,
            language = "zh",
            confidence = null,
            vadConfidence = null,
            isFinal = true,
            createdAt = startMs
        )
    }
}
