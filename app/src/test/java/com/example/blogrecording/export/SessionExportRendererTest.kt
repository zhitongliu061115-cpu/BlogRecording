package com.example.blogrecording.export

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.GeneratedTag
import com.example.blogrecording.data.GeneratedTagSource
import com.example.blogrecording.data.HighlightSource
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.SessionHighlights
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.SessionTagGeneration
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TagGenerationStatus
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExportRendererTest {
    @Test
    fun markdownIncludesAvailableSectionsInDeterministicOrder() {
        val payload = render(sampleDetail(), SessionExportFormat.MARKDOWN)

        assertTrue(payload.fileName.startsWith("Episode_One_Test-20231114_221320."))
        assertTrue(payload.content.contains("# Episode One/Test"))
        assertInOrder(
            payload.content,
            "## Summary",
            "## Timeline",
            "## Tags",
            "## Favorite Highlights",
            "## Transcript"
        )
        assertTrue(payload.content.contains("#AI #Podcast"))
        assertTrue(payload.content.contains("00:00:01-00:00:03 Intro"))
        assertTrue(payload.content.contains("[00:00:01 - 00:00:02] Speaker 1: hello"))
        assertFalse(payload.content.contains("not favorite"))
    }

    @Test
    fun txtOmitsMissingOptionalSections() {
        val detail = sampleDetail().copy(
            session = sampleDetail().session.copy(
                summary = null,
                tagGeneration = SessionTagGeneration.empty(),
                highlights = SessionHighlights.empty()
            ),
            transcriptSegments = listOf(transcript(1_000L, 2_000L, "only transcript"))
        )
        val payload = render(detail, SessionExportFormat.TXT)

        assertTrue(payload.content.contains("Transcript"))
        assertTrue(payload.content.contains("only transcript"))
        assertFalse(payload.content.contains("Tags"))
        assertFalse(payload.content.contains("Favorite Highlights"))
    }

    @Test
    fun jsonContainsVersionAndEscapesContentWithoutPrivateFields() {
        val detail = sampleDetail().copy(
            session = sampleDetail().session.copy(title = "Episode \"Quoted\"")
        )
        val payload = render(detail, SessionExportFormat.JSON)
        val json = JSONObject(payload.content)

        assertEquals(SessionExportRenderer.FORMAT_VERSION, json.getInt("formatVersion"))
        assertEquals("Episode \"Quoted\"", json.getJSONObject("session").getString("title"))
        assertEquals("AI", json.getJSONArray("tags").getString(0))
        assertEquals("hello", json.getJSONArray("transcript").getJSONObject(0).getString("text"))
        assertFalse(payload.content.contains("apiKey"))
        assertFalse(payload.content.contains("pcmFilePath"))
        assertFalse(payload.content.contains("audioFilePath"))
    }

    @Test
    fun emptyContentReturnsFailure() {
        val result = SessionExportRenderer.render(
            detail = sampleDetail().copy(
                session = sampleDetail().session.copy(
                    transcript = "",
                    summary = null,
                    tagGeneration = SessionTagGeneration.empty(),
                    highlights = SessionHighlights.empty()
                ),
                transcriptSegments = emptyList()
            ),
            format = SessionExportFormat.MARKDOWN,
            generatedAtMillis = NOW
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.ExportEmptyContent, (result as AppResult.Failure).error)
    }

    @Test
    fun filenameSanitizesReservedCharactersAndFallbacksBlankTitle() {
        assertEquals(
            "Bad_Title_Name-20231114_221320.md",
            SessionExportRenderer.exportFileName(" Bad:/Title*Name ", NOW, "md")
        )
        assertEquals(
            "podcast-session-20231114_221320.txt",
            SessionExportRenderer.exportFileName("   ", NOW, "txt")
        )
    }

    private fun render(
        detail: PodcastSessionDetail,
        format: SessionExportFormat
    ): SessionExportPayload {
        val result = SessionExportRenderer.render(detail, format, NOW)
        assertTrue(result is AppResult.Success)
        return (result as AppResult.Success).value
    }

    private fun sampleDetail(): PodcastSessionDetail {
        return PodcastSessionDetail(
            session = PodcastSession(
                id = "session-1",
                title = "Episode One/Test",
                createdAt = 1_000L,
                updatedAt = 2_000L,
                sourceType = AudioSourceType.LOCAL_MEDIA,
                status = PodcastSessionStatus.SUMMARIZED,
                activeSegmentId = null,
                lastCompletedSegmentId = null,
                transcript = "legacy transcript",
                summary = SessionSummary(
                    text = "fallback summary",
                    status = SummaryStatus.SUMMARIZED,
                    modelName = "deepseek-chat",
                    generatedAt = 2_000L,
                    updatedAt = 2_000L,
                    errorMessage = null,
                    structured = StructuredSummary(
                        overview = "overview",
                        keyPoints = listOf("point"),
                        actionItems = listOf("action"),
                        openQuestions = listOf("question"),
                        quoteCandidates = listOf("quote"),
                        timelineChapters = listOf(
                            TimelineChapter(
                                title = "Intro",
                                startMs = 1_000L,
                                endMs = 3_000L,
                                keyPoints = listOf("chapter point"),
                                sourceStartMs = 1_000L,
                                sourceEndMs = 3_000L
                            )
                        )
                    )
                ),
                summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
                summaryLanguage = SummaryLanguage.CHINESE,
                summaryModelName = "deepseek-chat",
                asrModelName = "SenseVoice",
                vadModelName = "VAD",
                diarizationModelName = "Diarization",
                detectedSpeakerCount = 1,
                recordingSegmentCount = 1,
                transcriptSegmentCount = 2,
                errorMessage = null,
                legacyRecordingSessionId = null,
                tagGeneration = SessionTagGeneration(
                    tags = listOf(
                        tag("Podcast", order = 1),
                        tag("AI", order = 0)
                    ),
                    status = TagGenerationStatus.GENERATED,
                    generatedAt = 2_000L,
                    updatedAt = 2_000L
                ),
                highlights = SessionHighlights(
                    items = listOf(
                        highlight("favorite highlight", isFavorite = true, startMs = 1_000L),
                        highlight("not favorite", isFavorite = false, startMs = 2_000L)
                    ),
                    generatedAt = 2_000L,
                    updatedAt = 2_000L
                )
            ),
            recordingSegments = emptyList(),
            transcriptSegments = listOf(
                transcript(2_000L, 3_000L, "world"),
                transcript(1_000L, 2_000L, "hello")
            ),
            speakerProfiles = emptyList()
        )
    }

    private fun transcript(startMs: Long, endMs: Long, text: String): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = "t-$startMs",
            sessionId = "session-1",
            recordingSegmentId = null,
            startMs = startMs,
            endMs = endMs,
            speakerId = "speaker-1",
            speakerDisplayName = "Speaker 1",
            text = text,
            language = null,
            confidence = null,
            vadConfidence = null,
            isFinal = true,
            createdAt = startMs
        )
    }

    private fun tag(text: String, order: Int): GeneratedTag {
        return GeneratedTag(
            text = text,
            normalizedKey = text.lowercase(),
            order = order,
            source = GeneratedTagSource.STRUCTURED_SUMMARY,
            generatedAt = 2_000L
        )
    }

    private fun highlight(
        text: String,
        isFavorite: Boolean,
        startMs: Long
    ): SessionHighlight {
        return SessionHighlight(
            id = "h-$startMs",
            text = text,
            normalizedKey = text,
            source = HighlightSource.STRUCTURED_SUMMARY,
            sourceStartMs = startMs,
            sourceEndMs = startMs + 1_000L,
            transcriptSegmentIds = emptyList(),
            isFavorite = isFavorite,
            generated = true,
            createdAt = startMs,
            updatedAt = startMs
        )
    }

    private fun assertInOrder(source: String, vararg parts: String) {
        var index = -1
        parts.forEach { part ->
            val next = source.indexOf(part)
            assertTrue("$part not found", next >= 0)
            assertTrue("$part out of order", next > index)
            index = next
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
