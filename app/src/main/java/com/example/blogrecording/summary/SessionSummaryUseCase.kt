package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.SessionRepository
import com.example.blogrecording.data.SummaryStatus
import kotlinx.coroutines.flow.first

class SessionSummaryUseCase(
    private val sessionRepository: SessionRepository,
    private val readApiKey: suspend () -> AppResult<String>,
    private val generateSummary: suspend (apiKey: String, transcript: String, settings: AppSettings) -> AppResult<String>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun start(
        sessionId: String,
        settings: AppSettings
    ): AppResult<PodcastSession> {
        val detail = sessionRepository.observeSessionDetail(sessionId).first()
            ?: return AppResult.Failure(AppError.Unknown("session missing"))
        val aggregateTranscript = SessionTranscriptAggregator.aggregate(detail)
        val eligibility = SessionSummaryEligibilityPolicy.evaluate(detail, aggregateTranscript)
        if (!eligibility.canStart) {
            return AppResult.Failure(AppError.Unknown(eligibility.disabledReason ?: "summary not ready"))
        }

        val apiKey = when (val result = readApiKey()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                markFailed(sessionId, settings, result.error)
                return AppResult.Failure(result.error.toSummaryUiError())
            }
        }

        when (val marking = sessionRepository.updateSummaryLifecycle(
            sessionId = sessionId,
            status = SummaryStatus.SUMMARIZING,
            modelName = settings.deepSeekModel
        )) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return marking
        }

        return when (val result = generateSummary(apiKey, aggregateTranscript, settings)) {
            is AppResult.Success -> sessionRepository.updateSummaryLifecycle(
                sessionId = sessionId,
                status = SummaryStatus.SUMMARIZED,
                modelName = settings.deepSeekModel,
                summaryText = result.value,
                generatedAt = nowMillis()
            )
            is AppResult.Failure -> {
                markFailed(sessionId, settings, result.error)
                AppResult.Failure(result.error.toSummaryUiError())
            }
        }
    }

    private suspend fun markFailed(
        sessionId: String,
        settings: AppSettings,
        error: AppError
    ) {
        sessionRepository.updateSummaryLifecycle(
            sessionId = sessionId,
            status = SummaryStatus.FAILED,
            modelName = settings.deepSeekModel,
            errorMessage = error.toSummaryLifecycleMessage()
        )
    }
}

private fun AppError.toSummaryUiError(): AppError {
    return when (this) {
        AppError.DeepSeekApiKeyMissing,
        AppError.DeepSeekUnauthorized,
        AppError.DeepSeekRateLimited,
        AppError.TranscriptTooLong -> this
        is AppError.NetworkFailed -> AppError.NetworkFailed("DeepSeek summary request failed")
        is AppError.Unknown -> AppError.Unknown("Summary failed")
        else -> AppError.Unknown("Summary failed")
    }
}

private fun AppError.toSummaryLifecycleMessage(): String {
    return when (this) {
        AppError.DeepSeekApiKeyMissing -> "DeepSeek API Key missing"
        AppError.DeepSeekUnauthorized -> "DeepSeek authentication failed"
        AppError.DeepSeekRateLimited -> "DeepSeek rate limited"
        AppError.TranscriptTooLong -> "Transcript too long"
        is AppError.NetworkFailed -> "DeepSeek summary request failed"
        is AppError.Unknown -> "Summary failed"
        else -> "Summary failed"
    }
}
