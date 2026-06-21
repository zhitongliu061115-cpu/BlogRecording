package com.example.blogrecording.qa

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
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionQaContextBuilderTest {
    @Test
    fun buildsCurrentSessionContextInPriorityOrder() {
        val context = SessionQaContextBuilder().build(detail(sessionId = "current"))

        assertInOrder(context.text, "Structured Summary", "Timeline", "Favorited Highlights", "Tags", "Transcript")
        assertTrue(context.text.contains("current transcript"))
        assertFalse(context.text.contains("not favorite"))
        assertFalse(context.text.contains("other session secret"))
    }

    @Test
    fun truncatesDeterministicallyAndKeepsSummaryFirst() {
        val context = SessionQaContextBuilder(maxChars = 140).build(
            detail(sessionId = "current", transcriptText = "long ".repeat(200))
        )

        assertTrue(context.wasTruncated)
        assertTrue(context.text.length <= 140)
        assertTrue(context.text.startsWith("Structured Summary"))
        assertTrue(context.text.contains("overview"))
    }

    @Test
    fun promptRestrictsAnswerToEpisodeContext() {
        val prompt = SessionQaPromptBuilder().build(
            question = "What is the action item?",
            context = SessionQaContext("Summary: only this episode", wasTruncated = false)
        )

        assertTrue(prompt.contains("Use only the episode context"))
        assertTrue(prompt.contains("does not provide enough information"))
        assertTrue(prompt.contains("What is the action item?"))
    }

    private fun detail(
        sessionId: String,
        transcriptText: String = "current transcript"
    ): PodcastSessionDetail {
        return PodcastSessionDetail(
            session = PodcastSession(
                id = sessionId,
                title = "Episode",
                createdAt = 1_000L,
                updatedAt = 2_000L,
                sourceType = AudioSourceType.MICROPHONE,
                status = PodcastSessionStatus.SUMMARIZED,
                activeSegmentId = null,
                lastCompletedSegmentId = null,
                transcript = "legacy transcript",
                summary = SessionSummary(
                    text = "summary",
                    status = SummaryStatus.SUMMARIZED,
                    modelName = "deepseek-chat",
                    generatedAt = 2_000L,
                    updatedAt = 2_000L,
                    errorMessage = null,
                    structured = StructuredSummary(
                        overview = "overview",
                        keyPoints = listOf("point"),
                        actionItems = listOf("action"),
                        openQuestions = emptyList(),
                        quoteCandidates = emptyList(),
                        timelineChapters = listOf(
                            TimelineChapter(
                                title = "Intro",
                                startMs = 1_000L,
                                endMs = 2_000L,
                                keyPoints = listOf("chapter point"),
                                sourceStartMs = 1_000L,
                                sourceEndMs = 2_000L
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
                transcriptSegmentCount = 1,
                errorMessage = null,
                legacyRecordingSessionId = null,
                tagGeneration = SessionTagGeneration(
                    tags = listOf(
                        GeneratedTag("AI", "ai", 0, GeneratedTagSource.STRUCTURED_SUMMARY, 2_000L)
                    )
                ),
                highlights = SessionHighlights(
                    items = listOf(
                        SessionHighlight(
                            id = "h-1",
                            text = "favorite quote",
                            normalizedKey = "favorite quote",
                            source = HighlightSource.TRANSCRIPT,
                            sourceStartMs = 1_000L,
                            sourceEndMs = 2_000L,
                            transcriptSegmentIds = listOf("t-1"),
                            isFavorite = true,
                            generated = true,
                            createdAt = 2_000L,
                            updatedAt = 2_000L
                        ),
                        SessionHighlight(
                            id = "h-2",
                            text = "not favorite",
                            normalizedKey = "not favorite",
                            source = HighlightSource.TRANSCRIPT,
                            sourceStartMs = 2_000L,
                            sourceEndMs = 3_000L,
                            transcriptSegmentIds = listOf("t-2"),
                            isFavorite = false,
                            generated = true,
                            createdAt = 3_000L,
                            updatedAt = 3_000L
                        )
                    )
                )
            ),
            recordingSegments = emptyList(),
            transcriptSegments = listOf(
                transcript(sessionId, 1_000L, transcriptText),
                transcript("other", 2_000L, "other session secret")
            ).filter { it.sessionId == sessionId },
            speakerProfiles = emptyList()
        )
    }

    private fun transcript(sessionId: String, startMs: Long, text: String): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = "t-$startMs",
            sessionId = sessionId,
            recordingSegmentId = null,
            startMs = startMs,
            endMs = startMs + 1_000L,
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

    private fun assertInOrder(source: String, vararg parts: String) {
        var index = -1
        parts.forEach { part ->
            val next = source.indexOf(part)
            assertTrue("$part not found", next >= 0)
            assertTrue("$part out of order", next > index)
            index = next
        }
    }
}
