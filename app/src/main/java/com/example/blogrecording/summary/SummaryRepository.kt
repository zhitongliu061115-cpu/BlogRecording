package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings

class SummaryRepository(
    private val client: DeepSeekSummaryClient,
    private val promptBuilder: SummaryPromptBuilder = SummaryPromptBuilder(),
    private val chunker: TranscriptChunker = TranscriptChunker()
) {
    suspend fun generateSummary(
        apiKey: String,
        transcript: String,
        settings: AppSettings
    ): AppResult<String> {
        when (val validation = SummaryGenerationPolicy.validateInputs(apiKey, transcript)) {
            is AppResult.Failure -> return validation
            is AppResult.Success -> Unit
        }
        val chunks = chunker.chunk(transcript)
        if (chunks.isEmpty()) return AppResult.Failure(AppError.Unknown("转写为空"))

        val partials = mutableListOf<String>()
        for (chunk in chunks) {
            val prompt = promptBuilder.buildChunkPrompt(chunk, settings.summaryLanguage, settings.summaryStyle)
            when (val result = client.summarize(apiKey, settings.deepSeekModel, prompt)) {
                is AppResult.Success -> partials += result.value
                is AppResult.Failure -> return result
            }
        }

        if (partials.size == 1) return AppResult.Success(partials.first())
        val finalPrompt = promptBuilder.buildFinalPrompt(partials, settings.summaryLanguage, settings.summaryStyle)
        return client.summarize(apiKey, settings.deepSeekModel, finalPrompt)
    }
}
