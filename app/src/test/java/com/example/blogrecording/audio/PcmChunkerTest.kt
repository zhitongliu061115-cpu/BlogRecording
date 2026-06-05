package com.example.blogrecording.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PcmChunkerTest {
    @Test
    fun offerEmitsFixedDurationChunksAndKeepsRemainder() {
        val chunker = PcmChunker(targetDurationMs = 1_000L)
        val samples = ShortArray(2_500) { it.toShort() }

        val chunks = chunker.offer(
            PcmAudioStream(
                samples = samples,
                sampleRate = 1_000,
                channelCount = 1,
                timestampMs = 123L
            )
        )

        assertEquals(2, chunks.size)
        assertEquals(500L, chunker.currentDurationMs)
        assertEquals(1, chunks[0].sequence)
        assertEquals(0L, chunks[0].startMs)
        assertEquals(1_000L, chunks[0].endMs)
        assertArrayEquals(samples.copyOfRange(0, 1_000), chunks[0].samples)
        assertEquals(2, chunks[1].sequence)
        assertEquals(1_000L, chunks[1].startMs)
        assertEquals(2_000L, chunks[1].endMs)
        assertArrayEquals(samples.copyOfRange(1_000, 2_000), chunks[1].samples)
    }

    @Test
    fun flushEmitsPartialChunkWithElapsedTimestamps() {
        val chunker = PcmChunker(targetDurationMs = 1_000L)
        val samples = ShortArray(1_250) { (it + 1).toShort() }

        val chunks = chunker.offer(
            PcmAudioStream(
                samples = samples,
                sampleRate = 1_000,
                channelCount = 1,
                timestampMs = 456L
            )
        )
        val partial = chunker.flush()

        assertEquals(1, chunks.size)
        requireNotNull(partial)
        assertEquals(2, partial.sequence)
        assertEquals(1_000L, partial.startMs)
        assertEquals(1_250L, partial.endMs)
        assertArrayEquals(samples.copyOfRange(1_000, 1_250), partial.samples)
        assertNull(chunker.flush())
    }

    @Test
    fun offerIgnoresEmptyStreams() {
        val chunker = PcmChunker(targetDurationMs = 1_000L)

        val chunks = chunker.offer(
            PcmAudioStream(
                samples = ShortArray(0),
                sampleRate = 16_000,
                channelCount = 1,
                timestampMs = 789L
            )
        )

        assertEquals(emptyList<PcmChunk>(), chunks)
        assertNull(chunker.flush())
    }

    @Test
    fun chunkCanBeSplitIntoRecognizerWindows() {
        val chunk = PcmChunk(
            sequence = 1,
            samples = ShortArray(2_500) { it.toShort() },
            sampleRate = 1_000,
            channelCount = 1,
            startMs = 2_000L,
            endMs = 4_500L
        )

        val segments = chunk.toVadSegments(maxDurationMs = 1_000L)

        assertEquals(3, segments.size)
        assertEquals(2_000L, segments[0].startMs)
        assertEquals(3_000L, segments[0].endMs)
        assertArrayEquals(chunk.samples.copyOfRange(0, 1_000), segments[0].samples)
        assertEquals(4_000L, segments[2].startMs)
        assertEquals(4_500L, segments[2].endMs)
        assertArrayEquals(chunk.samples.copyOfRange(2_000, 2_500), segments[2].samples)
    }
}
