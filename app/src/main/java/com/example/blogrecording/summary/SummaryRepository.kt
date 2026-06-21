package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.SessionTagGeneration
import com.example.blogrecording.data.SessionHighlights
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.SummaryStyle

data class SummaryGenerationResult(
    val text: String,
    val structured: StructuredSummary,
    val tagGeneration: SessionTagGeneration,
    val highlights: SessionHighlights
)

class SummaryRepository(
    private val client: DeepSeekSummaryClient,
    private val promptBuilder: SummaryPromptBuilder = SummaryPromptBuilder(),
    private val chunker: TranscriptChunker = TranscriptChunker(),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun generateSummary(
        apiKey: String,
        transcript: String,
        settings: AppSettings,
        overrideStyle: SummaryStyle? = null
    ): AppResult<SummaryGenerationResult> {
        when (val validation = SummaryGenerationPolicy.validateInputs(apiKey, transcript)) {
            is AppResult.Failure -> return validation
            is AppResult.Success -> Unit
        }
        val chunks = chunker.chunk(transcript)
        if (chunks.isEmpty()) return AppResult.Failure(AppError.Unknown("转写为空"))

        val effectiveStyle = overrideStyle ?: settings.summaryStyle

        val partials = mutableListOf<String>()
        for (chunk in chunks) {
            val prompt = promptBuilder.buildChunkPrompt(chunk, settings.summaryLanguage, effectiveStyle)
            when (val result = client.summarize(apiKey, settings.deepSeekModel, prompt)) {
                is AppResult.Success -> partials += result.value
                is AppResult.Failure -> return result
            }
        }

        val rawSummary = if (partials.size == 1) {
            partials.first()
        } else {
            val finalPrompt = promptBuilder.buildFinalPrompt(partials, settings.summaryLanguage, effectiveStyle)
            when (val final = client.summarize(apiKey, settings.deepSeekModel, finalPrompt)) {
                is AppResult.Success -> final.value
                is AppResult.Failure -> return final
            }
        }
        val structured = StructuredSummaryParser.parse(rawSummary)
        val generatedAt = nowMillis()
        return AppResult.Success(
            SummaryGenerationResult(
                text = structured.toDisplayText(rawSummary),
                structured = structured,
                tagGeneration = SessionTagGenerator.generate(
                    rawModelText = rawSummary,
                    structured = structured,
                    transcript = transcript,
                    generatedAt = generatedAt
                ),
                highlights = SessionHighlightGenerator.generate(
                    structured = structured,
                    transcriptSegments = emptyList(),
                    fallbackTranscript = transcript,
                    generatedAt = generatedAt
                )
            )
        )
    }

    private fun StructuredSummary.toDisplayText(rawSummary: String): String {
        if (overview.isNotBlank() && parseStatus != StructuredSummaryParseStatus.FALLBACK_TEXT) {
            return buildString {
                appendLine(overview)
                appendSection("关键要点", keyPoints)
                appendSection("行动项", actionItems)
                appendSection("开放问题", openQuestions)
                appendSection("金句候选", quoteCandidates)
            }.trim()
        }
        return overview.ifBlank { rawSummary.trim() }
    }

    private fun StringBuilder.appendSection(title: String, items: List<String>) {
        if (items.isEmpty()) return
        appendLine()
        appendLine(title)
        items.forEach { appendLine("- $it") }
    }
}
