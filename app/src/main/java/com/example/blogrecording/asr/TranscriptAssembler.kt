package com.example.blogrecording.asr

import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.diarization.SpeakerSegment
import java.util.UUID

class TranscriptAssembler {
    fun assemble(
        sessionId: String,
        vadStartMs: Long,
        vadEndMs: Long,
        asrResult: AsrResult,
        speaker: SpeakerSegment
    ): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            startMs = vadStartMs,
            endMs = vadEndMs,
            speakerId = speaker.speakerId,
            speakerDisplayName = speaker.displayName,
            text = asrResult.text,
            language = asrResult.language,
            confidence = asrResult.confidence,
            vadConfidence = speaker.vadConfidence,
            isFinal = asrResult.isFinal,
            createdAt = System.currentTimeMillis()
        )
    }
}
