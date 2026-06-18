package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryGenerationPolicyTest {
    @Test
    fun validationRejectsBlankApiKeyBeforeTranscript() {
        val result = SummaryGenerationPolicy.validateInputs(apiKey = " ", transcript = " ")

        assertEquals(AppResult.Failure(AppError.DeepSeekApiKeyMissing), result)
    }

    @Test
    fun validationRejectsBlankTranscript() {
        val result = SummaryGenerationPolicy.validateInputs(apiKey = "key", transcript = "\n ")

        assertEquals(
            AppResult.Failure(AppError.Unknown(SummaryGenerationPolicy.BLANK_TRANSCRIPT_MESSAGE)),
            result
        )
    }

    @Test
    fun validationAcceptsKeyAndTranscript() {
        val result = SummaryGenerationPolicy.validateInputs(apiKey = "key", transcript = "hello")

        assertEquals(AppResult.Success(Unit), result)
    }
}
