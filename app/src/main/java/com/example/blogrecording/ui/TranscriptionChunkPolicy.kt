package com.example.blogrecording.ui

import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.vad.VadSegment

internal object TranscriptionChunkPolicy {
    fun recognizerSegments(
        chunk: PcmChunk,
        hasMeaningfulAudio: (VadSegment) -> Boolean
    ): List<VadSegment> {
        return chunk
            .toVadSegments(LocalProcessingPolicy.MAX_RECOGNIZER_SEGMENT_MS)
            .filter(hasMeaningfulAudio)
    }
}
