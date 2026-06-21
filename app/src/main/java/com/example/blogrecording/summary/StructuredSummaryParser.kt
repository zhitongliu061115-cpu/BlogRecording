package com.example.blogrecording.summary

import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import org.json.JSONException
import org.json.JSONObject

object StructuredSummaryParser {
    fun parse(raw: String): StructuredSummary {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) {
            return fallback("")
        }
        val jsonText = extractJsonObject(cleaned) ?: return fallback(cleaned)
        val json = try {
            JSONObject(jsonText)
        } catch (_: JSONException) {
            return fallback(cleaned)
        }
        val overview = firstString(json, "overview", "summary", "oneSentence", "tlDr")
        val keyPoints = firstList(json, "keyPoints", "key_points", "points")
        val actionItems = firstList(json, "actionItems", "action_items", "actions")
        val openQuestions = firstList(json, "openQuestions", "open_questions", "questions")
        val quoteCandidates = firstList(json, "quoteCandidates", "quote_candidates", "quotes")
        if (
            overview.isBlank() &&
            keyPoints.isEmpty() &&
            actionItems.isEmpty() &&
            openQuestions.isEmpty() &&
            quoteCandidates.isEmpty()
        ) {
            return fallback(cleaned)
        }
        val parseStatus = if (
            overview.isBlank() ||
            keyPoints.isEmpty() ||
            actionItems.isEmpty() ||
            openQuestions.isEmpty() ||
            quoteCandidates.isEmpty()
        ) {
            StructuredSummaryParseStatus.PARTIAL
        } else {
            StructuredSummaryParseStatus.STRUCTURED
        }
        return StructuredSummary(
            overview = overview.ifBlank { keyPoints.firstOrNull().orEmpty() },
            keyPoints = keyPoints.take(MAX_ITEMS),
            actionItems = actionItems.take(MAX_ITEMS),
            openQuestions = openQuestions.take(MAX_ITEMS),
            quoteCandidates = quoteCandidates.take(MAX_ITEMS),
            parseStatus = parseStatus
        )
    }

    private fun fallback(text: String): StructuredSummary {
        return StructuredSummary(
            overview = text.take(MAX_FALLBACK_CHARS).trim(),
            keyPoints = emptyList(),
            actionItems = emptyList(),
            openQuestions = emptyList(),
            quoteCandidates = emptyList(),
            parseStatus = StructuredSummaryParseStatus.FALLBACK_TEXT
        )
    }

    private fun extractJsonObject(raw: String): String? {
        val fenced = Regex("```(?:json)?\\s*(\\{.*?})\\s*```", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
        if (!fenced.isNullOrBlank()) return fenced
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun firstString(json: JSONObject, vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            json.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
        }.orEmpty()
    }

    private fun firstList(json: JSONObject, vararg keys: String): List<String> {
        keys.forEach { key ->
            val array = json.optJSONArray(key)
            if (array != null) {
                return List(array.length()) { index -> array.optString(index) }
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != "null" }
            }
            val value = json.optString(key).trim()
            if (value.isNotBlank() && value != "null") return listOf(value)
        }
        return emptyList()
    }

    private const val MAX_ITEMS = 12
    private const val MAX_FALLBACK_CHARS = 8_000
}
