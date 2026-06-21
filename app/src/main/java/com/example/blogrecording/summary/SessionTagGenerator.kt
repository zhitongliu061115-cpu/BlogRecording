package com.example.blogrecording.summary

import com.example.blogrecording.data.GeneratedTag
import com.example.blogrecording.data.GeneratedTagSource
import com.example.blogrecording.data.SessionTagGeneration
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.TagGenerationStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

object SessionTagGenerator {
    const val MAX_TAGS = 8
    const val MAX_TAG_CHARS = 24

    fun generate(
        rawModelText: String,
        structured: StructuredSummary?,
        transcript: String,
        generatedAt: Long
    ): SessionTagGeneration {
        val source = if (structured.hasUsableStructuredContent()) {
            GeneratedTagSource.STRUCTURED_SUMMARY
        } else {
            GeneratedTagSource.TRANSCRIPT
        }
        val candidates = extractModelTags(rawModelText)
            .ifEmpty { structured.toStructuredCandidates() }
            .ifEmpty { transcript.toTranscriptCandidates() }
        val tags = normalize(candidates, source, generatedAt)
        return SessionTagGeneration(
            tags = tags,
            status = TagGenerationStatus.GENERATED,
            generatedAt = generatedAt,
            updatedAt = generatedAt,
            errorMessage = null
        )
    }

    fun blocked(
        status: TagGenerationStatus,
        updatedAt: Long,
        errorMessage: String? = null
    ): SessionTagGeneration {
        return SessionTagGeneration(
            tags = emptyList(),
            status = status,
            generatedAt = null,
            updatedAt = updatedAt,
            errorMessage = errorMessage?.take(MAX_ERROR_CHARS)?.takeIf { it.isNotBlank() }
        )
    }

    fun normalize(
        candidates: List<String>,
        source: GeneratedTagSource,
        generatedAt: Long
    ): List<GeneratedTag> {
        val seen = linkedSetOf<String>()
        val tags = mutableListOf<GeneratedTag>()
        for (candidate in candidates) {
            val text = candidate.cleanTagText()
            if (text.isBlank()) continue
            val bounded = text.take(MAX_TAG_CHARS).trim()
            val key = bounded.normalizedTagKey()
            if (key.isBlank() || !seen.add(key)) continue
            tags += GeneratedTag(
                text = bounded,
                normalizedKey = key,
                order = tags.size + 1,
                source = source,
                generatedAt = generatedAt,
                status = TagGenerationStatus.GENERATED
            )
            if (tags.size >= MAX_TAGS) break
        }
        return tags
    }

    private fun extractModelTags(raw: String): List<String> {
        val jsonText = extractJsonObject(raw) ?: return emptyList()
        val json = try {
            JSONObject(jsonText)
        } catch (_: JSONException) {
            return emptyList()
        }
        return firstArray(json, "tags", "keywords", "keywordTags", "keyword_tags")
    }

    private fun StructuredSummary?.hasUsableStructuredContent(): Boolean {
        return this != null &&
            parseStatus != StructuredSummaryParseStatus.FALLBACK_TEXT &&
            (overview.isNotBlank() || keyPoints.isNotEmpty() || actionItems.isNotEmpty())
    }

    private fun StructuredSummary?.toStructuredCandidates(): List<String> {
        if (!hasUsableStructuredContent()) return emptyList()
        val summary = requireNotNull(this)
        return listOf(summary.overview) + summary.keyPoints + summary.actionItems + summary.openQuestions
    }

    private fun String.toTranscriptCandidates(): List<String> {
        val words = lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in STOP_WORDS }
        val rankedWords = words
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        val phrases = split(Regex("[\\r\\n，。！？；,.!?;:]+"))
            .map { it.cleanTagText() }
            .filter { it.length in 3..MAX_TAG_CHARS }
        return rankedWords + phrases
    }

    private fun firstArray(json: JSONObject, vararg keys: String): List<String> {
        keys.forEach { key ->
            when (val value = json.opt(key)) {
                is JSONArray -> return List(value.length()) { index -> value.optString(index) }
                is String -> if (value.isNotBlank() && value != "null") return listOf(value)
            }
        }
        return emptyList()
    }

    private fun extractJsonObject(raw: String): String? {
        val fenced = Regex(
            "```(?:json)?\\s*(\\{.*?})\\s*```",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(raw)?.groupValues?.getOrNull(1)
        if (!fenced.isNullOrBlank()) return fenced
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun String.cleanTagText(): String {
        return replace(Regex("\\s+"), " ")
            .trim()
            .trim('#', '-', '*', ' ', '\t', '\n', '\r', '：', ':')
            .takeIf { it != "null" }
            .orEmpty()
    }

    private fun String.normalizedTagKey(): String {
        return lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
    }

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "that", "this", "from", "you", "are", "was", "were",
        "have", "has", "but", "not", "can", "about", "into", "your", "our", "their"
    )
    private const val MAX_ERROR_CHARS = 120
}
