package com.example.blogrecording.ui

import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.data.AudioSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionChunkPolicyTest {
    @Test
    fun silentChunkProducesNoRecognizerSegments() {
        val chunk = chunk(samples = ShortArray(16_000) { 0 })

        val segments = TranscriptionChunkPolicy.recognizerSegments(
            chunk = chunk,
            sourceType = AudioSourceType.INTERNAL_AUDIO
        ) { false }

        assertTrue(segments.isEmpty())
    }

    @Test
    fun lowAmplitudeInternalAudioKeepsRecognizerSegments() {
        val chunk = chunk(samples = ShortArray(16_000) { 2 })

        val segments = TranscriptionChunkPolicy.recognizerSegments(
            chunk = chunk,
            sourceType = AudioSourceType.INTERNAL_AUDIO
        ) { false }

        assertEquals(1, segments.size)
        assertEquals(0L, segments.single().startMs)
        assertEquals(1_000L, segments.single().endMs)
    }

    @Test
    fun microphoneChunkStillUsesMeaningfulAudioGate() {
        val chunk = chunk(samples = ShortArray(16_000) { 2 })

        val segments = TranscriptionChunkPolicy.recognizerSegments(
            chunk = chunk,
            sourceType = AudioSourceType.MICROPHONE
        ) { false }

        assertTrue(segments.isEmpty())
    }

    private fun chunk(samples: ShortArray): PcmChunk {
        return PcmChunk(
            sequence = 1,
            samples = samples,
            sampleRate = 16_000,
            channelCount = 1,
            startMs = 0L,
            endMs = 1_000L
        )
    }
}
