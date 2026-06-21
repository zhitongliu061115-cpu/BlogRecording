package com.example.blogrecording.export

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class SessionExportFormat(
    val extension: String,
    val mimeType: String
) {
    MARKDOWN("md", "text/markdown"),
    TXT("txt", "text/plain"),
    JSON("json", "application/json")
}

data class SessionExportPayload(
    val format: SessionExportFormat,
    val fileName: String,
    val mimeType: String,
    val content: String
)

object SessionExportRenderer {
    const val FORMAT_VERSION = 1

    private val fileTimestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC)

    fun render(
        detail: PodcastSessionDetail,
        format: SessionExportFormat,
        generatedAtMillis: Long
    ): AppResult<SessionExportPayload> {
        val content = when (format) {
            SessionExportFormat.MARKDOWN -> renderMarkdown(detail)
            SessionExportFormat.TXT -> renderTxt(detail)
            SessionExportFormat.JSON -> renderJson(detail, generatedAtMillis)
        }
        if (!detail.hasExportableContent() || content.isBlank()) {
            return AppResult.Failure(AppError.ExportEmptyContent)
        }
        return AppResult.Success(
            SessionExportPayload(
                format = format,
                fileName = exportFileName(
                    title = detail.session.title,
                    generatedAtMillis = generatedAtMillis,
                    extension = format.extension
                ),
                mimeType = format.mimeType,
                content = content
            )
        )
    }

    fun exportFileName(
        title: String,
        generatedAtMillis: Long,
        extension: String
    ): String {
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(80)
            .trim('_')
            .ifBlank { "podcast-session" }
        val timestamp = fileTimestampFormatter.format(Instant.ofEpochMilli(generatedAtMillis))
        return "$safeTitle-$timestamp.$extension"
    }

    private fun renderMarkdown(detail: PodcastSessionDetail): String {
        return buildString {
            appendLine("# ${detail.session.title.ifBlank { "Podcast Session" }}")
            appendLine()
            appendSummaryMarkdown(detail.session.summary?.structured, detail.session.summary?.text)
            appendTimelineMarkdown(detail.timelineChapters())
            appendTagsMarkdown(detail)
            appendHighlightsMarkdown(detail.favoriteHighlights())
            appendTranscriptMarkdown(detail)
        }.trimEnd()
    }

    private fun StringBuilder.appendSummaryMarkdown(
        structured: StructuredSummary?,
        fallbackText: String?
    ) {
        if (structured == null && fallbackText.isNullOrBlank()) return
        appendLine("## Summary")
        appendLine()
        if (structured == null) {
            appendLine(fallbackText.orEmpty())
            appendLine()
            return
        }
        appendLine(structured.overview.ifBlank { fallbackText.orEmpty() })
        appendLine()
        appendBullets("Key Points", structured.keyPoints)
        appendBullets("Action Items", structured.actionItems)
        appendBullets("Open Questions", structured.openQuestions)
        appendBullets("Quote Candidates", structured.quoteCandidates)
    }

    private fun StringBuilder.appendTimelineMarkdown(chapters: List<TimelineChapter>) {
        if (chapters.isEmpty()) return
        appendLine("## Timeline")
        appendLine()
        chapters.sortedBy { it.startMs ?: Long.MAX_VALUE }.forEach { chapter ->
            appendLine("### ${chapter.timeLabel()} ${chapter.title}".trim())
            chapter.keyPoints.forEach { point -> appendLine("- $point") }
            appendLine()
        }
    }

    private fun StringBuilder.appendTagsMarkdown(detail: PodcastSessionDetail) {
        val tags = detail.exportTags()
        if (tags.isEmpty()) return
        appendLine("## Tags")
        appendLine()
        appendLine(tags.joinToString(separator = " ") { "#$it" })
        appendLine()
    }

    private fun StringBuilder.appendHighlightsMarkdown(highlights: List<SessionHighlight>) {
        if (highlights.isEmpty()) return
        appendLine("## Favorite Highlights")
        appendLine()
        highlights.forEach { highlight ->
            val time = highlight.timeLabel()?.let { "[$it] " }.orEmpty()
            appendLine("- $time${highlight.text}")
        }
        appendLine()
    }

    private fun StringBuilder.appendTranscriptMarkdown(detail: PodcastSessionDetail) {
        val transcript = detail.transcriptLines()
        if (transcript.isEmpty()) return
        appendLine("## Transcript")
        appendLine()
        transcript.forEach { appendLine(it) }
        appendLine()
    }

    private fun StringBuilder.appendBullets(title: String, items: List<String>) {
        if (items.isEmpty()) return
        appendLine("### $title")
        items.forEach { appendLine("- $it") }
        appendLine()
    }

    private fun renderTxt(detail: PodcastSessionDetail): String {
        return buildString {
            appendLine(detail.session.title.ifBlank { "Podcast Session" })
            appendLine()
            val summary = detail.session.summary
            val structured = summary?.structured
            if (structured != null || !summary?.text.isNullOrBlank()) {
                appendLine("Summary")
                appendLine(structured?.overview?.ifBlank { summary?.text.orEmpty() } ?: summary?.text.orEmpty())
                appendTxtList("Key Points", structured?.keyPoints.orEmpty())
                appendTxtList("Action Items", structured?.actionItems.orEmpty())
                appendTxtList("Open Questions", structured?.openQuestions.orEmpty())
                appendTxtList("Quote Candidates", structured?.quoteCandidates.orEmpty())
                appendLine()
            }
            val chapters = detail.timelineChapters()
            if (chapters.isNotEmpty()) {
                appendLine("Timeline")
                chapters.sortedBy { it.startMs ?: Long.MAX_VALUE }.forEach { chapter ->
                    appendLine("${chapter.timeLabel()} ${chapter.title}".trim())
                    chapter.keyPoints.forEach { appendLine("- $it") }
                }
                appendLine()
            }
            val tags = detail.exportTags()
            if (tags.isNotEmpty()) {
                appendLine("Tags")
                appendLine(tags.joinToString(", "))
                appendLine()
            }
            val highlights = detail.favoriteHighlights()
            if (highlights.isNotEmpty()) {
                appendLine("Favorite Highlights")
                highlights.forEach { highlight ->
                    val time = highlight.timeLabel()?.let { "[$it] " }.orEmpty()
                    appendLine("- $time${highlight.text}")
                }
                appendLine()
            }
            val transcript = detail.transcriptLines()
            if (transcript.isNotEmpty()) {
                appendLine("Transcript")
                transcript.forEach { appendLine(it) }
            }
        }.trimEnd()
    }

    private fun StringBuilder.appendTxtList(title: String, items: List<String>) {
        if (items.isEmpty()) return
        appendLine(title)
        items.forEach { appendLine("- $it") }
    }

    private fun renderJson(
        detail: PodcastSessionDetail,
        generatedAtMillis: Long
    ): String {
        val summary = detail.session.summary
        val structured = summary?.structured
        val root = JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("generatedAt", generatedAtMillis)
            .put(
                "session",
                JSONObject()
                    .put("title", detail.session.title)
                    .put("createdAt", detail.session.createdAt)
                    .put("updatedAt", detail.session.updatedAt)
            )
            .put(
                "summary",
                JSONObject()
                    .put("text", summary?.text)
                    .put(
                        "structured",
                        structured?.let {
                            JSONObject()
                                .put("overview", it.overview)
                                .put("keyPoints", JSONArray(it.keyPoints))
                                .put("actionItems", JSONArray(it.actionItems))
                                .put("openQuestions", JSONArray(it.openQuestions))
                                .put("quoteCandidates", JSONArray(it.quoteCandidates))
                        }
                    )
            )
            .put(
                "timeline",
                JSONArray(detail.timelineChapters().sortedBy { it.startMs ?: Long.MAX_VALUE }.map { chapter ->
                    JSONObject()
                        .put("title", chapter.title)
                        .put("startMs", chapter.startMs)
                        .put("endMs", chapter.endMs)
                        .put("keyPoints", JSONArray(chapter.keyPoints))
                })
            )
            .put("tags", JSONArray(detail.exportTags()))
            .put(
                "favoriteHighlights",
                JSONArray(detail.favoriteHighlights().map { highlight ->
                    JSONObject()
                        .put("text", highlight.text)
                        .put("sourceStartMs", highlight.sourceStartMs)
                        .put("sourceEndMs", highlight.sourceEndMs)
                })
            )
            .put(
                "transcript",
                JSONArray(detail.exportTranscriptSegments().map { segment ->
                    JSONObject()
                        .put("startMs", segment.startMs)
                        .put("endMs", segment.endMs)
                        .put("speaker", segment.speakerDisplayName)
                        .put("text", segment.text)
                })
            )
        return root.toString(2)
    }

    private fun PodcastSessionDetail.hasExportableContent(): Boolean {
        return session.transcript.isNotBlank() ||
            transcriptSegments.any { it.text.isNotBlank() } ||
            !session.summary?.text.isNullOrBlank() ||
            session.summary?.structured?.hasContent() == true ||
            exportTags().isNotEmpty() ||
            favoriteHighlights().isNotEmpty()
    }

    private fun StructuredSummary.hasContent(): Boolean {
        return overview.isNotBlank() ||
            keyPoints.isNotEmpty() ||
            actionItems.isNotEmpty() ||
            openQuestions.isNotEmpty() ||
            quoteCandidates.isNotEmpty() ||
            timelineChapters.isNotEmpty()
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

    private fun PodcastSessionDetail.exportTranscriptSegments(): List<TranscriptSegmentEntity> {
        return transcriptSegments
            .filter { it.text.isNotBlank() }
            .sortedBy { it.startMs }
    }

    private fun PodcastSessionDetail.transcriptLines(): List<String> {
        val segments = exportTranscriptSegments()
        if (segments.isNotEmpty()) {
            return segments.map { segment ->
                "[${segment.startMs.formatExportMs()} - ${segment.endMs.formatExportMs()}] " +
                    "${segment.speakerDisplayName}: ${segment.text}"
            }
        }
        return session.transcript
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun TimelineChapter.timeLabel(): String {
        val start = startMs?.formatExportMs()
        val end = endMs?.formatExportMs()
        return when {
            start != null && end != null -> "$start-$end"
            start != null -> start
            else -> ""
        }
    }

    private fun SessionHighlight.timeLabel(): String? {
        val start = sourceStartMs?.formatExportMs()
        val end = sourceEndMs?.formatExportMs()
        return when {
            start != null && end != null -> "$start-$end"
            start != null -> start
            else -> null
        }
    }

    private fun Long.formatExportMs(): String {
        val totalSeconds = this / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
