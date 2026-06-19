package com.example.blogrecording.ui

import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.vad.VadSegment

internal object TranscriptionChunkPolicy {
    fun speechSegments(
        chunk: PcmChunk,
        hasSpeech: (VadSegment) -> Boolean
    ): List<VadSegment> {
        return chunk
            .toVadSegments(LocalProcessingPolicy.MAX_RECOGNIZER_SEGMENT_MS)
            .filter(hasSpeech)
    }
}
