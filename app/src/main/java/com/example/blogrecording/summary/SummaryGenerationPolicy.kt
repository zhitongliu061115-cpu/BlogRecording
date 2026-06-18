package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult

internal object SummaryGenerationPolicy {
    const val BLANK_TRANSCRIPT_MESSAGE = "转写为空"

    fun validateInputs(apiKey: String, transcript: String): AppResult<Unit> {
        return when {
            apiKey.isBlank() -> AppResult.Failure(AppError.DeepSeekApiKeyMissing)
            transcript.isBlank() -> AppResult.Failure(AppError.Unknown(BLANK_TRANSCRIPT_MESSAGE))
            else -> AppResult.Success(Unit)
        }
    }
}
