package com.example.blogrecording.qa

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.QaMessageStatus
import com.example.blogrecording.data.SessionQaHistory
import com.example.blogrecording.data.SessionQaMessage
import com.example.blogrecording.data.SessionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class SessionQaUseCase(
    private val sessionRepository: SessionRepository,
    private val readApiKey: suspend () -> AppResult<String>,
    private val answerQuestion: suspend (
        apiKey: String,
        model: String,
        prompt: String
    ) -> AppResult<String>,
    private val contextBuilder: SessionQaContextBuilder = SessionQaContextBuilder(),
    private val promptBuilder: SessionQaPromptBuilder = SessionQaPromptBuilder(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun ask(
        sessionId: String,
        question: String,
        settings: AppSettings
    ): AppResult<PodcastSession> {
        val normalizedQuestion = question.trim()
        if (normalizedQuestion.isBlank()) {
            return AppResult.Failure(AppError.Unknown("Question is blank"))
        }
        val detail = sessionRepository.observeSessionDetail(sessionId).first()
            ?: return AppResult.Failure(AppError.Unknown("session missing"))
        val context = contextBuilder.build(detail)
        if (context.text.isBlank()) {
            val blocked = appendMessage(
                history = detail.session.qaHistory,
                question = normalizedQuestion,
                modelName = settings.deepSeekModel,
                status = QaMessageStatus.BLOCKED_EMPTY_CONTENT,
                errorMessage = "Session content required"
            )
            sessionRepository.updateQaHistory(sessionId, blocked)
            return AppResult.Failure(AppError.QaEmptyContent)
        }
        val apiKey = when (val result = readApiKey()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                val blocked = appendMessage(
                    history = detail.session.qaHistory,
                    question = normalizedQuestion,
                    modelName = settings.deepSeekModel,
                    status = QaMessageStatus.BLOCKED_MISSING_API_KEY,
                    errorMessage = "DeepSeek API Key missing"
                )
                sessionRepository.updateQaHistory(sessionId, blocked)
                return AppResult.Failure(result.error)
            }
        }

        val answering = appendMessage(
            history = detail.session.qaHistory,
            question = normalizedQuestion,
            modelName = settings.deepSeekModel,
            status = QaMessageStatus.ANSWERING,
            errorMessage = null
        )
        val marking = sessionRepository.updateQaHistory(sessionId, answering)
        if (marking is AppResult.Failure) return marking
        val prompt = promptBuilder.build(normalizedQuestion, context)
        return when (val result = answerQuestion(apiKey, settings.deepSeekModel, prompt)) {
            is AppResult.Success -> updateMessage(
                sessionId = sessionId,
                history = answering,
                messageId = answering.messages.last().id,
                status = QaMessageStatus.ANSWERED,
                answer = result.value,
                errorMessage = null
            )
            is AppResult.Failure -> {
                updateMessage(
                    sessionId = sessionId,
                    history = answering,
                    messageId = answering.messages.last().id,
                    status = QaMessageStatus.FAILED,
                    answer = null,
                    errorMessage = result.error.toQaHistoryError()
                )
                AppResult.Failure(result.error.toQaUiError())
            }
        }
    }

    suspend fun retry(
        sessionId: String,
        messageId: String,
        settings: AppSettings
    ): AppResult<PodcastSession> {
        val detail = sessionRepository.observeSessionDetail(sessionId).first()
            ?: return AppResult.Failure(AppError.Unknown("session missing"))
        val message = detail.session.qaHistory.messages.firstOrNull { it.id == messageId }
            ?: return AppResult.Failure(AppError.Unknown("QA message missing"))
        return ask(sessionId, message.question, settings)
    }

    private fun appendMessage(
        history: SessionQaHistory,
        question: String,
        modelName: String,
        status: QaMessageStatus,
        errorMessage: String?
    ): SessionQaHistory {
        val now = nowMillis()
        return history.copy(
            messages = history.messages + SessionQaMessage(
                id = newId(),
                question = question,
                answer = null,
                askedAt = now,
                answeredAt = null,
                status = status,
                modelName = modelName,
                errorMessage = errorMessage
            ),
            updatedAt = now
        )
    }

    private suspend fun updateMessage(
        sessionId: String,
        history: SessionQaHistory,
        messageId: String,
        status: QaMessageStatus,
        answer: String?,
        errorMessage: String?
    ): AppResult<PodcastSession> {
        val now = nowMillis()
        val updated = history.copy(
            messages = history.messages.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        answer = answer,
                        answeredAt = now,
                        status = status,
                        errorMessage = errorMessage
                    )
                } else {
                    message
                }
            },
            updatedAt = now
        )
        return sessionRepository.updateQaHistory(sessionId, updated)
    }
}

private fun AppError.toQaUiError(): AppError {
    return when (this) {
        AppError.DeepSeekApiKeyMissing,
        AppError.DeepSeekUnauthorized,
        AppError.DeepSeekRateLimited,
        AppError.QaEmptyContent -> this
        is AppError.NetworkFailed -> AppError.NetworkFailed("DeepSeek QA request failed")
        is AppError.Unknown -> AppError.Unknown("QA failed")
        else -> AppError.Unknown("QA failed")
    }
}

private fun AppError.toQaHistoryError(): String {
    return when (this) {
        AppError.DeepSeekApiKeyMissing -> "DeepSeek API Key missing"
        AppError.DeepSeekUnauthorized -> "DeepSeek authentication failed"
        AppError.DeepSeekRateLimited -> "DeepSeek rate limited"
        is AppError.NetworkFailed -> "DeepSeek QA request failed"
        is AppError.Unknown -> "QA failed"
        else -> "QA failed"
    }
}
