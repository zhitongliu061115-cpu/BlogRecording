package com.example.blogrecording.ui

import com.example.blogrecording.vad.VadSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProcessingPolicyTest {
    @Test
    fun recognizerWindowRemainsThirtySeconds() {
        assertEquals(30_000L, LocalProcessingPolicy.MAX_RECOGNIZER_SEGMENT_MS)
    }

    @Test
    fun longDiarizationSegmentUsesUnstableSpeakerOneFallback() {
        val fallback = LocalProcessingPolicy.speakerFallbackForLongSegment(
            VadSegment(
                samples = ShortArray(0),
                sampleRate = 16_000,
                startMs = 0L,
                endMs = 60_001L,
                pcmSampleCount = 0,
                confidence = null
            )
        )

        requireNotNull(fallback)
        assertEquals("speaker_1", fallback.speakerId)
        assertEquals("Speaker 1", fallback.displayName)
        assertTrue(fallback.unstable)
        assertNull(fallback.vadConfidence)
    }

    @Test
    fun diarizationBoundaryAtSixtySecondsStillUsesEngine() {
        val fallback = LocalProcessingPolicy.speakerFallbackForLongSegment(
            VadSegment(
                samples = ShortArray(0),
                sampleRate = 16_000,
                startMs = 0L,
                endMs = 60_000L,
                pcmSampleCount = 0,
                confidence = null
            )
        )

        assertNull(fallback)
    }
}
