package com.example.blogrecording.asr

import com.example.blogrecording.diarization.SpeakerSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptAssemblerTest {
    @Test
    fun assembleAssociatesTranscriptWithRecordingSegmentWhenProvided() {
        val assembler = TranscriptAssembler()

        val segment = assembler.assemble(
            sessionId = "session-1",
            recordingSegmentId = "recording-segment-1",
            vadStartMs = 1_000L,
            vadEndMs = 2_000L,
            asrResult = AsrResult(
                text = "hello",
                language = "en",
                confidence = 0.9f,
                isFinal = true
            ),
            speaker = SpeakerSegment(
                speakerId = "speaker_1",
                displayName = "Speaker 1",
                unstable = false,
                vadConfidence = 0.8f
            )
        )

        assertEquals("session-1", segment.sessionId)
        assertEquals("recording-segment-1", segment.recordingSegmentId)
        assertEquals("hello", segment.text)
    }
}
