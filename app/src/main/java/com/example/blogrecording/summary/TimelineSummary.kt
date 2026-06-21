package com.example.blogrecording.summary

import com.example.blogrecording.data.TimelineChapter
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.json.JSONArray
import org.json.JSONObject

object TimelineContextBuilder {
    fun build(segments: List<TranscriptSegmentEntity>): String {
        return usableSegments(segments).joinToString(separator = "\n") { segment ->
            "[${segment.startMs.formatMs()} - ${segment.endMs.formatMs()}] ${segment.speakerDisplayName}: ${segment.text}"
        }
    }

    fun bounds(segments: List<TranscriptSegmentEntity>): TimelineBounds? {
        val usable = usableSegments(segments)
        if (usable.isEmpty()) return null
        return TimelineBounds(
            startMs = usable.minOf { it.startMs },
            endMs = usable.maxOf { it.endMs }
        )
    }

    private fun usableSegments(segments: List<TranscriptSegmentEntity>): List<TranscriptSegmentEntity> {
        return segments
            .filter { it.startMs >= 0L && it.endMs > it.startMs && it.text.isNotBlank() }
            .sortedBy { it.startMs }
    }
}

data class TimelineBounds(
    val startMs: Long,
    val endMs: Long
)

object TimelineChapterParser {
    fun parse(raw: String, bounds: TimelineBounds?): List<TimelineChapter> {
        val jsonText = extractJsonObject(raw) ?: return emptyList()
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return emptyList()
        val chapters = root.optJSONArray("timelineChapters")
            ?: root.optJSONArray("chapters")
            ?: return emptyList()
        return parseChapters(chapters, bounds)
    }

    private fun parseChapters(array: JSONArray, bounds: TimelineBounds?): List<TimelineChapter> {
        var previousEnd: Long? = null
        val candidates = (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)
        }.sortedWith(compareBy<JSONObject> { it.nullableLong("startMs") ?: Long.MAX_VALUE })
        val parsed = mutableListOf<TimelineChapter>()
        for (json in candidates) {
            val title = json.optString("title").trim().takeIf { it.isNotBlank() } ?: continue
            val keyPoints = json.stringList("keyPoints").ifEmpty { json.stringList("points") }
            val rawStart = json.nullableLong("startMs")
            val rawEnd = json.nullableLong("endMs")
            val (start, end) = normalizeRange(rawStart, rawEnd, previousEnd, bounds)
            if (end != null) previousEnd = end
            parsed += TimelineChapter(
                title = title,
                startMs = start,
                endMs = end,
                keyPoints = keyPoints.take(8),
                sourceStartMs = start,
                sourceEndMs = end
            )
        }
        return parsed.sortedWith(compareBy<TimelineChapter> { it.startMs ?: Long.MAX_VALUE }.thenBy { it.title })
    }

    private fun normalizeRange(
        startMs: Long?,
        endMs: Long?,
        previousEndMs: Long?,
        bounds: TimelineBounds?
    ): Pair<Long?, Long?> {
        if (startMs == null || endMs == null) return null to null
        if (startMs < 0L || endMs <= startMs) return null to null
        if (bounds != null && (startMs < bounds.startMs || endMs > bounds.endMs)) return null to null
        if (previousEndMs != null && startMs < previousEndMs) return null to null
        return startMs to endMs
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun JSONObject.nullableLong(name: String): Long? {
        return optLong(name).takeUnless { isNull(name) }
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "null" }
    }
}

private fun Long.formatMs(): String {
    val totalSeconds = this / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
