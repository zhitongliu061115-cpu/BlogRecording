package com.example.blogrecording.ui

import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.vad.VadSegment

internal object TranscriptionChunkPolicy {
    fun recognizerSegments(
        chunk: PcmChunk,
        sourceType: AudioSourceType,
        hasMeaningfulAudio: (VadSegment) -> Boolean
    ): List<VadSegment> {
        return chunk
            .toVadSegments(LocalProcessingPolicy.MAX_RECOGNIZER_SEGMENT_MS)
            .filter { segment ->
                when (sourceType) {
                    AudioSourceType.INTERNAL_AUDIO -> segment.hasAnyCapturedWaveform()
                    AudioSourceType.MICROPHONE -> hasMeaningfulAudio(segment)
                }
            }
    }

    private fun VadSegment.hasAnyCapturedWaveform(): Boolean {
        return samples.any { kotlin.math.abs(it.toInt()) > INTERNAL_AUDIO_NOISE_FLOOR }
    }

    private const val INTERNAL_AUDIO_NOISE_FLOOR = 1
}
