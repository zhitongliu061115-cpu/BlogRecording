package com.example.blogrecording.summary

import com.example.blogrecording.data.TranscriptSegmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSummaryTest {
    @Test
    fun buildsTimestampedContextFromOrderedSegments() {
        val context = TimelineContextBuilder.build(
            listOf(
                transcript(startMs = 10_000L, endMs = 12_000L, text = "second"),
                transcript(startMs = 1_000L, endMs = 2_000L, text = "first")
            )
        )

        assertTrue(context.indexOf("first") < context.indexOf("second"))
        assertTrue(context.contains("[00:00:01 - 00:00:02]"))
        assertTrue(context.contains("Speaker 1: first"))
    }

    @Test
    fun missingOrInvalidTimestampsAreIgnored() {
        val context = TimelineContextBuilder.build(
            listOf(
                transcript(startMs = 0L, endMs = 0L, text = "bad"),
                transcript(startMs = 5_000L, endMs = 6_000L, text = "good")
            )
        )

        assertTrue(!context.contains("bad"))
        assertTrue(context.contains("good"))
        assertEquals(TimelineBounds(5_000L, 6_000L), TimelineContextBuilder.bounds(
            listOf(
                transcript(startMs = 0L, endMs = 0L, text = "bad"),
                transcript(startMs = 5_000L, endMs = 6_000L, text = "good")
            )
        ))
    }

    @Test
    fun parsesAndOrdersValidTimelineChapters() {
        val chapters = TimelineChapterParser.parse(
            """
            {"timelineChapters":[
              {"title":"B","startMs":10000,"endMs":12000,"keyPoints":["second"]},
              {"title":"A","startMs":1000,"endMs":3000,"keyPoints":["first"]}
            ]}
            """.trimIndent(),
            bounds = TimelineBounds(0L, 20_000L)
        )

        assertEquals(listOf("A", "B"), chapters.map { it.title })
        assertEquals(1_000L, chapters.first().startMs)
        assertEquals(listOf("first"), chapters.first().keyPoints)
    }

    @Test
    fun invalidRangesFallBackToUntimedChapters() {
        val chapters = TimelineChapterParser.parse(
            """
            {"timelineChapters":[
              {"title":"Reversed","startMs":5000,"endMs":1000,"keyPoints":["bad"]},
              {"title":"Out","startMs":1000,"endMs":50000,"keyPoints":["bad"]}
            ]}
            """.trimIndent(),
            bounds = TimelineBounds(0L, 10_000L)
        )

        assertEquals(2, chapters.size)
        assertNull(chapters.first().startMs)
        assertNull(chapters.first().endMs)
    }

    @Test
    fun structuredParserKeepsTimelineChapters() {
        val summary = StructuredSummaryParser.parse(
            """
            {
              "overview":"overview",
              "keyPoints":["point"],
              "actionItems":[],
              "openQuestions":[],
              "quoteCandidates":[],
              "timelineChapters":[{"title":"Intro","startMs":0,"endMs":1000,"keyPoints":["hello"]}]
            }
            """.trimIndent()
        )

        assertEquals("Intro", summary.timelineChapters.single().title)
        assertEquals(0L, summary.timelineChapters.single().startMs)
    }

    private fun transcript(
        startMs: Long,
        endMs: Long,
        text: String
    ): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = "t-$startMs",
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
