package com.example.blogrecording.ui

import com.example.blogrecording.audio.PcmChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionChunkPolicyTest {
    @Test
    fun silentChunkProducesNoRecognizerSegments() {
        val chunk = chunk(samples = ShortArray(16_000) { 0 })

        val segments = TranscriptionChunkPolicy.speechSegments(chunk) { false }

        assertTrue(segments.isEmpty())
    }

    @Test
    fun speechChunkKeepsRecognizerSegments() {
        val chunk = chunk(samples = ShortArray(16_000) { 1_000 })

        val segments = TranscriptionChunkPolicy.speechSegments(chunk) { true }

        assertEquals(1, segments.size)
        assertEquals(0L, segments.single().startMs)
        assertEquals(1_000L, segments.single().endMs)
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
