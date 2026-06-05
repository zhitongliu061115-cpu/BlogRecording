package com.example.blogrecording.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptChunkerTest {
    @Test
    fun chunkReturnsEmptyListForBlankTranscript() {
        val chunks = TranscriptChunker(maxCharsPerChunk = 10).chunk("  \n ")

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun chunkKeepsSmallTranscriptAsSingleChunk() {
        val chunks = TranscriptChunker(maxCharsPerChunk = 100).chunk("hello")

        assertEquals(listOf("hello"), chunks)
    }

    @Test
    fun chunkSplitsByParagraphWithoutDroppingText() {
        val transcript = "a".repeat(8) + "\n\n" + "b".repeat(8) + "\n\n" + "c".repeat(8)

        val chunks = TranscriptChunker(maxCharsPerChunk = 18).chunk(transcript)

        assertEquals(2, chunks.size)
        assertEquals(transcript.replace("\n\n", ""), chunks.joinToString("").replace("\n\n", ""))
        assertTrue(chunks.all { it.length <= 18 })
    }
}
