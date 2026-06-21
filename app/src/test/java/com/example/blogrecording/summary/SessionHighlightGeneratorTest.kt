package com.example.blogrecording.summary

import com.example.blogrecording.data.HighlightSource
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.SessionHighlights
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionHighlightGeneratorTest {
    @Test
    fun quoteCandidatesWinAndDeduplicateByText() {
        val highlights = SessionHighlightGenerator.generate(
            structured = structuredSummary(quoteCandidates = listOf("  quote one  ", "quote one", "quote two")),
            transcriptSegments = listOf(transcript(text = "transcript quote")),
            fallbackTranscript = "fallback",
            generatedAt = 1_000L
        )

        assertEquals(listOf("quote one", "quote two"), highlights.items.map { it.text })
        assertTrue(highlights.items.all { it.source == HighlightSource.STRUCTURED_SUMMARY })
        assertFalse(highlights.items.any { it.isFavorite })
    }

    @Test
    fun transcriptFallbackKeepsSourceMetadata() {
        val highlights = SessionHighlightGenerator.generate(
            structured = structuredSummary(quoteCandidates = emptyList()),
            transcriptSegments = listOf(
                transcript(id = "t-1", startMs = 1_000L, endMs = 2_000L, text = "first line"),
                transcript(id = "t-2", startMs = 2_000L, endMs = 3_000L, text = "second line")
            ),
            fallbackTranscript = "",
            generatedAt = 2_000L
        )

        assertEquals(listOf("first line", "second line"), highlights.items.map { it.text })
        assertEquals(1_000L, highlights.items.first().sourceStartMs)
        assertEquals(listOf("t-1"), highlights.items.first().transcriptSegmentIds)
        assertTrue(highlights.items.all { it.source == HighlightSource.TRANSCRIPT })
    }

    @Test
    fun regenerationPreservesFavoriteState() {
        val first = SessionHighlightGenerator.generate(
            structured = structuredSummary(quoteCandidates = listOf("favorite quote", "old quote")),
            transcriptSegments = emptyList(),
            fallbackTranscript = "",
            generatedAt = 1_000L
        )
        val favoriteId = first.items.first().id
        val favorited = SessionHighlightGenerator.toggleFavorite(first, favoriteId, nowMillis = 1_500L)

        val regenerated = SessionHighlightGenerator.generate(
            structured = structuredSummary(quoteCandidates = listOf("favorite quote", "new quote")),
            transcriptSegments = emptyList(),
            fallbackTranscript = "",
            existing = favorited,
            generatedAt = 2_000L
        )

        val favorite = regenerated.items.single { it.text == "favorite quote" }
        assertEquals(favoriteId, favorite.id)
        assertTrue(favorite.isFavorite)
        assertTrue(regenerated.items.any { it.text == "new quote" })
    }

    @Test
    fun toggleFavoriteSwitchesPersistentState() {
        val highlights = SessionHighlightGenerator.generate(
            structured = structuredSummary(quoteCandidates = listOf("quote")),
            transcriptSegments = emptyList(),
            fallbackTranscript = "",
            generatedAt = 1_000L
        )

        val toggled = SessionHighlightGenerator.toggleFavorite(highlights, highlights.items.single().id, 2_000L)
        val untoggled = SessionHighlightGenerator.toggleFavorite(toggled, highlights.items.single().id, 3_000L)

        assertTrue(toggled.items.single().isFavorite)
        assertFalse(untoggled.items.single().isFavorite)
        assertEquals(3_000L, untoggled.updatedAt)
    }

    private fun structuredSummary(quoteCandidates: List<String>): StructuredSummary {
        return StructuredSummary(
            overview = "overview",
            keyPoints = emptyList(),
            actionItems = emptyList(),
            openQuestions = emptyList(),
            quoteCandidates = quoteCandidates,
            parseStatus = StructuredSummaryParseStatus.STRUCTURED
        )
    }

    private fun transcript(
        id: String = "t-1",
        startMs: Long = 1_000L,
        endMs: Long = 2_000L,
        text: String
    ): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = id,
            sessionId = "session-1",
            recordingSegmentId = null,
            startMs = startMs,
            endMs = endMs,
            speakerId = "speaker-1",
            speakerDisplayName = "Speaker 1",
            text = text,
            language = "zh",
            confidence = null,
            vadConfidence = null,
            isFinal = true,
            createdAt = startMs
        )
    }
}
