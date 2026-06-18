package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastSessionMigrationTest {
    @Test
    fun legacyRecordingMapsToPodcastSessionWithoutLosingUserVisibleData() {
        val legacy = legacySession(
            status = RecordingStatus.COMPLETED,
            transcript = "Speaker 1: hello",
            summary = "summary",
            segmentCount = 3
        )

        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

        assertEquals(legacy.id, migrated.id)
        assertEquals(legacy.id, migrated.legacyRecordingSessionId)
        assertEquals(legacy.title, migrated.title)
        assertEquals(legacy.createdAt, migrated.createdAt)
        assertEquals(legacy.updatedAt, migrated.updatedAt)
        assertEquals(legacy.sourceType, migrated.sourceType)
        assertEquals(legacy.transcript, migrated.transcript)
        assertEquals(legacy.summary, migrated.summary?.text)
        assertEquals(legacy.summaryModelName, migrated.summaryModelName)
        assertEquals(legacy.summaryStyle, migrated.summaryStyle)
        assertEquals(legacy.summaryLanguage, migrated.summaryLanguage)
        assertEquals(legacy.detectedSpeakerCount, migrated.detectedSpeakerCount)
        assertEquals(1, migrated.recordingSegmentCount)
        assertEquals(3, migrated.transcriptSegmentCount)
        assertEquals(PodcastSessionStatus.SUMMARIZED, migrated.status)
    }

    @Test
    fun legacySessionWithoutTranscriptDoesNotInventRecordingSegment() {
        val legacy = legacySession(
            status = RecordingStatus.NOT_STARTED,
            transcript = "",
            summary = null,
            segmentCount = 0
        )

        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

        assertEquals(PodcastSessionStatus.DRAFT, migrated.status)
        assertEquals(0, migrated.recordingSegmentCount)
        assertEquals(0, migrated.transcriptSegmentCount)
        assertNull(migrated.summary)
    }

    @Test
    fun completedLegacyRecordWithSummaryMapsToSummarized() {
        val legacy = legacySession(
            status = RecordingStatus.COMPLETED,
            transcript = "transcript",
            summary = "summary",
            segmentCount = 2
        )

        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

        assertEquals(PodcastSessionStatus.SUMMARIZED, migrated.status)
        assertEquals(SummaryStatus.SUMMARIZED, migrated.summary?.status)
        assertEquals("summary", migrated.summary?.text)
    }

    @Test
    fun completedLegacyRecordWithTranscriptButNoSummaryMapsToReadyForSummary() {
        val legacy = legacySession(
            status = RecordingStatus.COMPLETED,
            transcript = "transcript",
            summary = null,
            segmentCount = 2
        )

        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

        assertEquals(PodcastSessionStatus.READY_FOR_SUMMARY, migrated.status)
        assertNull(migrated.summary)
        assertEquals("transcript", migrated.transcript)
    }

    @Test
    fun completedLegacyRecordWithoutTranscriptMapsToPaused() {
        val legacy = legacySession(
            status = RecordingStatus.COMPLETED,
            transcript = "",
            summary = null,
            segmentCount = 0
        )

        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

        assertEquals(PodcastSessionStatus.PAUSED, migrated.status)
        assertEquals("", migrated.transcript)
    }

    @Test
    fun interruptedLegacyStatusesMapToNonRecordingErrorAndPreserveTranscript() {
        val interruptedStatuses = listOf(
            RecordingStatus.CAPTURING_AUDIO,
            RecordingStatus.VAD_DETECTING,
            RecordingStatus.DIARIZING,
            RecordingStatus.TRANSCRIBING,
            RecordingStatus.SUMMARIZING
        )

        interruptedStatuses.forEach { status ->
            val legacy = legacySession(
                status = status,
                transcript = "saved transcript for $status",
                summary = null,
                segmentCount = 1
            )

            val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)

            assertEquals(PodcastSessionStatus.ERROR, migrated.status)
            assertNull(migrated.activeSegmentId)
            assertEquals("saved transcript for $status", migrated.transcript)
            assertEquals(1, migrated.recordingSegmentCount)
        }
    }

    @Test
    fun recoveryClearsActiveRecordingAndMarksActiveSegmentInterrupted() {
        val session = podcastSession(
            status = PodcastSessionStatus.RECORDING,
            activeSegmentId = "segment-1",
            recordingSegmentCount = 1
        )
        val activeSegment = recordingSegment(id = "segment-1", status = RecordingSegmentStatus.RECORDING)
        val completedSegment = recordingSegment(id = "segment-0", status = RecordingSegmentStatus.COMPLETED)

        val recovered = PodcastSessionMigration.recoverInterrupted(
            session = session,
            recordingSegments = listOf(activeSegment, completedSegment)
        )

        assertEquals(PodcastSessionStatus.PAUSED, recovered.session.status)
        assertNull(recovered.session.activeSegmentId)
        assertEquals(RecordingSegmentStatus.INTERRUPTED, recovered.recordingSegments.first().status)
        assertEquals(RecordingSegmentStatus.COMPLETED, recovered.recordingSegments.last().status)
        assertTrue(recovered.recordingSegments.first().errorMessage?.contains("Interrupted") == true)
    }

    @Test
    fun migratedDetailPreservesTranscriptAndSpeakerAssociations() {
        val legacy = legacySession(
            status = RecordingStatus.COMPLETED,
            transcript = "Speaker 1: hello",
            summary = null,
            segmentCount = 1
        )
        val migrated = PodcastSessionMigration.fromLegacyRecordingSession(legacy)
        val transcriptSegment = TranscriptSegmentEntity(
            id = "transcript-1",
            sessionId = legacy.id,
            recordingSegmentId = null,
            startMs = 0L,
            endMs = 1_000L,
            speakerId = "speaker_1",
            speakerDisplayName = "Speaker 1",
            text = "hello",
            language = "zh",
            confidence = null,
            vadConfidence = null,
            isFinal = true,
            createdAt = 300L
        )
        val speaker = SpeakerProfileEntity(
            id = "profile-1",
            sessionId = legacy.id,
            speakerId = "speaker_1",
            displayName = "Speaker 1",
            colorIndex = 0,
            segmentCount = 1,
            totalSpeechDurationMs = 1_000L,
            createdAt = 300L,
            updatedAt = 300L
        )

        val detail = PodcastSessionDetail(
            session = migrated,
            recordingSegments = emptyList(),
            transcriptSegments = listOf(transcriptSegment),
            speakerProfiles = listOf(speaker)
        )

        assertEquals(legacy.id, detail.session.id)
        assertEquals(legacy.id, detail.transcriptSegments.single().sessionId)
        assertEquals(legacy.id, detail.speakerProfiles.single().sessionId)
        assertEquals("hello", detail.transcriptSegments.single().text)
        assertEquals("Speaker 1", detail.speakerProfiles.single().displayName)
    }

    private fun legacySession(
        status: RecordingStatus,
        transcript: String,
        summary: String?,
        segmentCount: Int
    ): RecordingSessionEntity {
        return RecordingSessionEntity(
            id = "legacy-1",
            title = "Legacy Episode",
            createdAt = 100L,
            updatedAt = 200L,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            transcript = transcript,
            summary = summary,
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            summaryModelName = "deepseek-chat",
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            detectedSpeakerCount = 2,
            segmentCount = segmentCount,
            errorMessage = null
        )
    }

    private fun podcastSession(
        status: PodcastSessionStatus,
        activeSegmentId: String?,
        recordingSegmentCount: Int
    ): PodcastSession {
        return PodcastSession(
            id = "session-1",
            title = "Episode",
            createdAt = 100L,
            updatedAt = 200L,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            activeSegmentId = activeSegmentId,
            lastCompletedSegmentId = null,
            transcript = "previous transcript",
            summary = null,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 1,
            recordingSegmentCount = recordingSegmentCount,
            transcriptSegmentCount = 1,
            errorMessage = null,
            legacyRecordingSessionId = null
        )
    }

    private fun recordingSegment(
        id: String,
        status: RecordingSegmentStatus
    ): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = "session-1",
            index = 1,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            startedAt = 100L,
            endedAt = null,
            durationMs = 0L,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = 16_000,
            channelCount = 1,
            transcriptSegmentIds = emptyList(),
            errorMessage = null,
            createdAt = 100L,
            updatedAt = 100L
        )
    }
}
