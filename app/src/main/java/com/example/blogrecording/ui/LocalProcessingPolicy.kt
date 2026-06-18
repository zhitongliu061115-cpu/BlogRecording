package com.example.blogrecording.ui

import com.example.blogrecording.diarization.SpeakerSegment
import com.example.blogrecording.vad.VadSegment

internal object LocalProcessingPolicy {
    const val MAX_RECOGNIZER_SEGMENT_MS = 30_000L
    const val MAX_DIARIZATION_SEGMENT_MS = 60_000L

    fun speakerFallbackForLongSegment(segment: VadSegment): SpeakerSegment? {
        return if (segment.endMs - segment.startMs > MAX_DIARIZATION_SEGMENT_MS) {
            SpeakerSegment(
                speakerId = "speaker_1",
                displayName = "Speaker 1",
                unstable = true,
                vadConfidence = null
            )
        } else {
            null
        }
    }
}
