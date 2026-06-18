package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingEntityCompatibilityTest {
    @Test
    fun recordingSessionEntityKeepsLegacyFieldsAndNullableValues() {
        val session = RecordingSessionEntity(
            id = "session-1",
            title = "Episode notes",
            createdAt = 100L,
            updatedAt = 200L,
            sourceType = AudioSourceType.MICROPHONE,
            status = RecordingStatus.COMPLETED,
            transcript = "Speaker 1: hello",
            summary = null,
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            summaryModelName = "deepseek-chat",
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            detectedSpeakerCount = 2,
            segmentCount = 3,
            errorMessage = null
        )

        assertEquals("session-1", session.id)
        assertEquals("Episode notes", session.title)
        assertEquals(100L, session.createdAt)
        assertEquals(200L, session.updatedAt)
        assertEquals(AudioSourceType.MICROPHONE, session.sourceType)
        assertEquals(RecordingStatus.COMPLETED, session.status)
        assertEquals("Speaker 1: hello", session.transcript)
        assertNull(session.summary)
        assertEquals("deepseek-chat", session.summaryModelName)
        assertEquals(SummaryStyle.POINTS_QUOTES_ACTIONS, session.summaryStyle)
        assertEquals(SummaryLanguage.CHINESE, session.summaryLanguage)
        assertEquals(2, session.detectedSpeakerCount)
        assertEquals(3, session.segmentCount)
        assertNull(session.errorMessage)
    }

    @Test
    fun transcriptSegmentEntityKeepsAsrAndSpeakerMetadata() {
        val segment = TranscriptSegmentEntity(
            id = "transcript-1",
            sessionId = "session-1",
            recordingSegmentId = "recording-segment-1",
            startMs = 1_000L,
            endMs = 2_500L,
            speakerId = "speaker_1",
            speakerDisplayName = "Speaker 1",
            text = "hello",
            language = "zh",
            confidence = 0.9f,
            vadConfidence = 0.8f,
            isFinal = true,
            createdAt = 3_000L
        )

        assertEquals("transcript-1", segment.id)
        assertEquals("session-1", segment.sessionId)
        assertEquals("recording-segment-1", segment.recordingSegmentId)
        assertEquals(1_000L, segment.startMs)
        assertEquals(2_500L, segment.endMs)
        assertEquals("speaker_1", segment.speakerId)
        assertEquals("Speaker 1", segment.speakerDisplayName)
        assertEquals("hello", segment.text)
        assertEquals("zh", segment.language)
        assertEquals(0.9f, segment.confidence)
        assertEquals(0.8f, segment.vadConfidence)
        assertEquals(true, segment.isFinal)
        assertEquals(3_000L, segment.createdAt)
    }

    @Test
    fun speakerProfileEntityKeepsDerivedSpeakerSummaryFields() {
        val speaker = SpeakerProfileEntity(
            id = "profile-1",
            sessionId = "session-1",
            speakerId = "speaker_1",
            displayName = "Speaker 1",
            colorIndex = 0,
            segmentCount = 4,
            totalSpeechDurationMs = 12_000L,
            createdAt = 100L,
            updatedAt = 200L
        )

        assertEquals("profile-1", speaker.id)
        assertEquals("session-1", speaker.sessionId)
        assertEquals("speaker_1", speaker.speakerId)
        assertEquals("Speaker 1", speaker.displayName)
        assertEquals(0, speaker.colorIndex)
        assertEquals(4, speaker.segmentCount)
        assertEquals(12_000L, speaker.totalSpeechDurationMs)
        assertEquals(100L, speaker.createdAt)
        assertEquals(200L, speaker.updatedAt)
    }
}
