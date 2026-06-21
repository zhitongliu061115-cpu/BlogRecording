package com.example.blogrecording.summary

import com.example.blogrecording.data.GeneratedTagSource
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.TagGenerationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTagGeneratorTest {
    @Test
    fun modelTagsWinAndAreNormalized() {
        val result = SessionTagGenerator.generate(
            rawModelText = """{"tags":[" AI ","ai","Podcast","${"x".repeat(40)}"," "]}""",
            structured = structuredSummary(),
            transcript = "ignored transcript",
            generatedAt = 1_000L
        )

        assertEquals(TagGenerationStatus.GENERATED, result.status)
        assertEquals(listOf("AI", "Podcast", "x".repeat(SessionTagGenerator.MAX_TAG_CHARS)), result.tags.map { it.text })
        assertEquals(listOf("ai", "podcast", "x".repeat(SessionTagGenerator.MAX_TAG_CHARS)), result.tags.map { it.normalizedKey })
        assertTrue(result.tags.all { it.source == GeneratedTagSource.STRUCTURED_SUMMARY })
    }

    @Test
    fun structuredSummaryIsPreferredWhenModelTagsAreMissing() {
        val result = SessionTagGenerator.generate(
            rawModelText = """{"overview":"topic"}""",
            structured = structuredSummary(
                overview = "AI workflow",
                keyPoints = listOf("Podcast production", "AI workflow")
            ),
            transcript = "transcript fallback",
            generatedAt = 2_000L
        )

        assertEquals(listOf("AI workflow", "Podcast production"), result.tags.map { it.text })
        assertTrue(result.tags.all { it.source == GeneratedTagSource.STRUCTURED_SUMMARY })
    }

    @Test
    fun transcriptFallbackRanksRepeatedWordsAndCapsCount() {
        val result = SessionTagGenerator.generate(
            rawModelText = "plain text",
            structured = StructuredSummary(
                overview = "plain text",
                keyPoints = emptyList(),
                actionItems = emptyList(),
                openQuestions = emptyList(),
                quoteCandidates = emptyList(),
                parseStatus = StructuredSummaryParseStatus.FALLBACK_TEXT
            ),
            transcript = "kotlin kotlin android summary android podcast export timeline highlight qa qa compose tags chapters audio",
            generatedAt = 3_000L
        )

        assertEquals(SessionTagGenerator.MAX_TAGS, result.tags.size)
        assertEquals("android", result.tags.first().text)
        assertTrue(result.tags.all { it.source == GeneratedTagSource.TRANSCRIPT })
    }

    @Test
    fun blockedStateDoesNotLeakContent() {
        val result = SessionTagGenerator.blocked(
            status = TagGenerationStatus.BLOCKED_MISSING_API_KEY,
            updatedAt = 4_000L,
            errorMessage = "x".repeat(200)
        )

        assertEquals(TagGenerationStatus.BLOCKED_MISSING_API_KEY, result.status)
        assertTrue(result.tags.isEmpty())
        assertEquals(120, result.errorMessage?.length)
    }

    private fun structuredSummary(
        overview: String = "overview",
        keyPoints: List<String> = listOf("point")
    ): StructuredSummary {
        return StructuredSummary(
            overview = overview,
            keyPoints = keyPoints,
            actionItems = emptyList(),
            openQuestions = emptyList(),
            quoteCandidates = emptyList(),
            parseStatus = StructuredSummaryParseStatus.STRUCTURED
        )
    }
}
