package com.example.blogrecording.summary

import com.example.blogrecording.data.StructuredSummaryParseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredSummaryParserTest {
    @Test
    fun parsesStructuredJsonSummary() {
        val summary = StructuredSummaryParser.parse(
            """
            {
              "overview": "one line",
              "keyPoints": ["point"],
              "actionItems": ["action"],
              "openQuestions": ["question"],
              "quoteCandidates": ["quote"]
            }
            """.trimIndent()
        )

        assertEquals(StructuredSummaryParseStatus.STRUCTURED, summary.parseStatus)
        assertEquals("one line", summary.overview)
        assertEquals(listOf("point"), summary.keyPoints)
        assertEquals(listOf("action"), summary.actionItems)
        assertEquals(listOf("question"), summary.openQuestions)
        assertEquals(listOf("quote"), summary.quoteCandidates)
    }

    @Test
    fun parsesFencedJsonAndPartialStructure() {
        val summary = StructuredSummaryParser.parse(
            """
            ```json
            {"overview":"overview","key_points":["point"]}
            ```
            """.trimIndent()
        )

        assertEquals(StructuredSummaryParseStatus.PARTIAL, summary.parseStatus)
        assertEquals("overview", summary.overview)
        assertEquals(listOf("point"), summary.keyPoints)
        assertTrue(summary.actionItems.isEmpty())
    }

    @Test
    fun parsesFencedJsonWithNestedTimelineChapters() {
        val summary = StructuredSummaryParser.parse(
            """
            model note
            ```json
            {
              "overview": "overview",
              "keyPoints": ["point"],
              "actionItems": ["action"],
              "openQuestions": ["question"],
              "quoteCandidates": ["quote"],
              "timelineChapters": [
                {
                  "title": "Intro",
                  "startMs": 0,
                  "endMs": 1000,
                  "keyPoints": ["nested object"]
                }
              ]
            }
            ```
            """.trimIndent()
        )

        assertEquals(StructuredSummaryParseStatus.STRUCTURED, summary.parseStatus)
        assertEquals("overview", summary.overview)
        assertEquals("Intro", summary.timelineChapters.single().title)
    }

    @Test
    fun fallsBackToPlainTextWhenJsonIsMalformed() {
        val summary = StructuredSummaryParser.parse("plain text summary")

        assertEquals(StructuredSummaryParseStatus.FALLBACK_TEXT, summary.parseStatus)
        assertEquals("plain text summary", summary.overview)
        assertTrue(summary.keyPoints.isEmpty())
    }
}
