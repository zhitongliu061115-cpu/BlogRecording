package com.example.blogrecording.qa

import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.TranscriptSegmentEntity

data class SessionQaContext(
    val text: String,
    val wasTruncated: Boolean
)

class SessionQaContextBuilder(
    private val maxChars: Int = DEFAULT_MAX_CHARS
) {
    fun build(detail: PodcastSessionDetail): SessionQaContext {
        val prioritySections = buildList {
            detail.session.summary?.structured?.let { add(structuredSummarySection(it)) }
            detail.session.summary?.text?.takeIf { it.isNotBlank() }?.let { add("Summary:\n$it") }
            detail.timelineChapters().takeIf { it.isNotEmpty() }?.let { add(timelineSection(it)) }
            detail.favoriteHighlights().takeIf { it.isNotEmpty() }?.let { add(highlightsSection(it)) }
            detail.exportTags().takeIf { it.isNotEmpty() }?.let { add("Tags:\n${it.joinToString(", ")}") }
        }.filter { it.isNotBlank() }

        val base = prioritySections.joinToString("\n\n").trim()
        val transcript = transcriptSection(detail)
        val combined = listOf(base, transcript)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
        if (combined.length <= maxChars) {
            return SessionQaContext(combined, wasTruncated = false)
        }

        val boundedBase = base.take(maxChars).trimEnd()
        if (boundedBase.length >= maxChars || transcript.isBlank()) {
            return SessionQaContext(boundedBase, wasTruncated = true)
        }
        val remaining = (maxChars - boundedBase.length - 2).coerceAtLeast(0)
        val truncated = listOf(
            boundedBase,
            transcript.take(remaining).trimEnd()
        ).filter { it.isNotBlank() }.joinToString("\n\n")
        return SessionQaContext(truncated, wasTruncated = true)
    }

    private fun structuredSummarySection(summary: StructuredSummary): String {
        return buildString {
            appendLine("Structured Summary:")
            appendLine(summary.overview)
            appendList("Key Points", summary.keyPoints)
            appendList("Action Items", summary.actionItems)
            appendList("Open Questions", summary.openQuestions)
            appendList("Quote Candidates", summary.quoteCandidates)
        }.trim()
    }

    private fun timelineSection(chapters: List<TimelineChapter>): String {
        return buildString {
            appendLine("Timeline:")
            chapters.sortedBy { it.startMs ?: Long.MAX_VALUE }.forEach { chapter ->
                appendLine("- ${chapter.timeLabel()} ${chapter.title}".trim())
                chapter.keyPoints.forEach { appendLine("  - $it") }
            }
        }.trim()
    }

    private fun highlightsSection(highlights: List<SessionHighlight>): String {
        return buildString {
            appendLine("Favorited Highlights:")
            highlights.forEach { highlight ->
                val time = highlight.timeLabel()?.let { "[$it] " }.orEmpty()
                appendLine("- $time${highlight.text}")
            }
        }.trim()
    }

    private fun transcriptSection(detail: PodcastSessionDetail): String {
        val segments = detail.transcriptSegments
            .filter { it.text.isNotBlank() }
            .sortedBy { it.startMs }
        if (segments.isEmpty()) {
            return detail.session.transcript.takeIf { it.isNotBlank() }?.let { "Transcript:\n$it" }.orEmpty()
        }
        return buildString {
            appendLine("Transcript:")
            segments.forEach { segment ->
                appendLine("[${segment.startMs.formatQaMs()} - ${segment.endMs.formatQaMs()}] ${segment.speakerDisplayName}: ${segment.text}")
            }
        }.trim()
    }

    private fun PodcastSessionDetail.timelineChapters(): List<TimelineChapter> {
        return session.summary?.structured?.timelineChapters.orEmpty()
    }

    private fun PodcastSessionDetail.exportTags(): List<String> {
        return session.tagGeneration.tags
            .sortedBy { it.order }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun PodcastSessionDetail.favoriteHighlights(): List<SessionHighlight> {
        return session.highlights.items
            .filter { it.isFavorite && it.text.isNotBlank() }
            .sortedWith(compareBy<SessionHighlight> { it.sourceStartMs ?: Long.MAX_VALUE }.thenBy { it.createdAt })
    }

    private fun StringBuilder.appendList(title: String, items: List<String>) {
        if (items.isEmpty()) return
        appendLine(title)
        items.forEach { appendLine("- $it") }
    }

    private fun TimelineChapter.timeLabel(): String {
        val start = startMs?.formatQaMs()
        val end = endMs?.formatQaMs()
        return when {
            start != null && end != null -> "$start-$end"
            start != null -> start
            else -> ""
        }
    }

    private fun SessionHighlight.timeLabel(): String? {
        val start = sourceStartMs?.formatQaMs()
        val end = sourceEndMs?.formatQaMs()
        return when {
            start != null && end != null -> "$start-$end"
            start != null -> start
            else -> null
        }
    }

    private fun Long.formatQaMs(): String {
        val totalSeconds = this / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private companion object {
        const val DEFAULT_MAX_CHARS = 16_000
    }
}
