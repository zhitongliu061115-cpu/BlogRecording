package com.example.blogrecording.summary

import com.example.blogrecording.data.HighlightSource
import com.example.blogrecording.data.SessionHighlight
import com.example.blogrecording.data.SessionHighlights
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.TranscriptSegmentEntity
import java.util.Locale

object SessionHighlightGenerator {
    const val MAX_HIGHLIGHTS = 8
    const val MAX_HIGHLIGHT_CHARS = 180

    fun generate(
        structured: StructuredSummary?,
        transcriptSegments: List<TranscriptSegmentEntity>,
        fallbackTranscript: String,
        existing: SessionHighlights = SessionHighlights.empty(),
        generatedAt: Long
    ): SessionHighlights {
        val candidates = structured.toQuoteCandidates()
            .map { Candidate(text = it, source = HighlightSource.STRUCTURED_SUMMARY) }
            .ifEmpty {
                transcriptSegments
                    .filter { it.text.isNotBlank() }
                    .sortedBy { it.startMs }
                    .map {
                        Candidate(
                            text = it.text,
                            source = HighlightSource.TRANSCRIPT,
                            sourceStartMs = it.startMs,
                            sourceEndMs = it.endMs,
                            transcriptSegmentIds = listOf(it.id)
                        )
                    }
                    .ifEmpty {
                        fallbackTranscript.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map { Candidate(text = it, source = HighlightSource.TRANSCRIPT) }
                    }
            }

        val existingFavorites = existing.items.filter { it.isFavorite }
        val favoritesByKey = existingFavorites.associateBy { it.normalizedKey }
        val generated = normalize(candidates, generatedAt).map { candidate ->
            favoritesByKey[candidate.normalizedKey]?.let { favorite ->
                candidate.copy(
                    id = favorite.id,
                    isFavorite = true,
                    createdAt = favorite.createdAt,
                    updatedAt = generatedAt
                )
            } ?: candidate
        }
        val generatedKeys = generated.map { it.normalizedKey }.toSet()
        val preservedFavorites = existingFavorites.filter { it.normalizedKey !in generatedKeys }
        return SessionHighlights(
            items = (generated + preservedFavorites).sortedWith(
                compareByDescending<SessionHighlight> { it.isFavorite }.thenBy { it.createdAt }.thenBy { it.text }
            ),
            generatedAt = generatedAt,
            updatedAt = generatedAt
        )
    }

    fun toggleFavorite(
        highlights: SessionHighlights,
        highlightId: String,
        nowMillis: Long
    ): SessionHighlights {
        return highlights.copy(
            items = highlights.items.map { highlight ->
                if (highlight.id == highlightId) {
                    highlight.copy(isFavorite = !highlight.isFavorite, updatedAt = nowMillis)
                } else {
                    highlight
                }
            },
            updatedAt = nowMillis
        )
    }

    private fun normalize(candidates: List<Candidate>, generatedAt: Long): List<SessionHighlight> {
        val seen = linkedSetOf<String>()
        val highlights = mutableListOf<SessionHighlight>()
        for (candidate in candidates) {
            val text = candidate.text.cleanHighlightText()
            if (text.isBlank()) continue
            val bounded = text.take(MAX_HIGHLIGHT_CHARS).trim()
            val normalized = normalizedKey(
                text = bounded,
                sourceStartMs = candidate.sourceStartMs,
                sourceEndMs = candidate.sourceEndMs
            )
            if (!seen.add(normalized)) continue
            highlights += SessionHighlight(
                id = "highlight-${stableHash(normalized)}",
                text = bounded,
                normalizedKey = normalized,
                source = candidate.source,
                sourceStartMs = candidate.sourceStartMs,
                sourceEndMs = candidate.sourceEndMs,
                transcriptSegmentIds = candidate.transcriptSegmentIds,
                isFavorite = false,
                generated = true,
                createdAt = generatedAt + highlights.size,
                updatedAt = generatedAt
            )
            if (highlights.size >= MAX_HIGHLIGHTS) break
        }
        return highlights
    }

    private fun StructuredSummary?.toQuoteCandidates(): List<String> {
        return if (this != null && parseStatus != StructuredSummaryParseStatus.FALLBACK_TEXT) {
            quoteCandidates
        } else {
            emptyList()
        }
    }

    private fun normalizedKey(text: String, sourceStartMs: Long?, sourceEndMs: Long?): String {
        val normalizedText = text.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
        return listOfNotNull(normalizedText, sourceStartMs?.toString(), sourceEndMs?.toString())
            .joinToString("|")
    }

    private fun String.cleanHighlightText(): String {
        return replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '“', '”', '-', '*', ' ')
            .takeIf { it != "null" }
            .orEmpty()
    }

    private fun stableHash(value: String): String {
        return value.hashCode().toUInt().toString(16)
    }

    private data class Candidate(
        val text: String,
        val source: HighlightSource,
        val sourceStartMs: Long? = null,
        val sourceEndMs: Long? = null,
        val transcriptSegmentIds: List<String> = emptyList()
    )
}
