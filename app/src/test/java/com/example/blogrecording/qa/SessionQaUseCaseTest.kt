package com.example.blogrecording.qa

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.QaMessageStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.SessionQaHistory
import com.example.blogrecording.data.SessionRepository
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionQaUseCaseTest {
    @Test
    fun successfulAnswerPersistsQaHistory() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail(transcriptSegments = listOf(transcript("episode content"))))
        var capturedPrompt = ""
        val useCase = useCase(
            repository = repository,
            answerQuestion = { _, _, prompt ->
                capturedPrompt = prompt
                AppResult.Success("episode answer")
            }
        )

        val result = useCase.ask("session-1", " What happened? ", settings())
        val message = (result as AppResult.Success).value.qaHistory.messages.single()

        assertEquals(QaMessageStatus.ANSWERED, message.status)
        assertEquals("What happened?", message.question)
        assertEquals("episode answer", message.answer)
        assertEquals("deepseek-chat", message.modelName)
        assertTrue(capturedPrompt.contains("episode content"))
        assertTrue(capturedPrompt.contains("What happened?"))
    }

    @Test
    fun missingApiKeyBlocksBeforeNetwork() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail(transcriptSegments = listOf(transcript("episode content"))))
        var calls = 0
        val useCase = useCase(
            repository = repository,
            readApiKey = { AppResult.Failure(AppError.DeepSeekApiKeyMissing) },
            answerQuestion = { _, _, _ ->
                calls += 1
                AppResult.Success("no")
            }
        )

        val result = useCase.ask("session-1", "Question?", settings())
        val latest = repository.detail("session-1")!!.session

        assertEquals(AppResult.Failure(AppError.DeepSeekApiKeyMissing), result)
        assertEquals(0, calls)
        assertEquals(QaMessageStatus.BLOCKED_MISSING_API_KEY, latest.qaHistory.messages.single().status)
    }

    @Test
    fun missingContentBlocksBeforeApiKeyRead() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail())
        var keyReads = 0
        val useCase = useCase(
            repository = repository,
            readApiKey = {
                keyReads += 1
                AppResult.Success("key")
            }
        )

        val result = useCase.ask("session-1", "Question?", settings())
        val latest = repository.detail("session-1")!!.session

        assertEquals(AppResult.Failure(AppError.QaEmptyContent), result)
        assertEquals(0, keyReads)
        assertEquals(QaMessageStatus.BLOCKED_EMPTY_CONTENT, latest.qaHistory.messages.single().status)
    }

    @Test
    fun providerFailurePersistsRetryableSanitizedError() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail(transcriptSegments = listOf(transcript("private transcript"))))
        val useCase = useCase(
            repository = repository,
            answerQuestion = { _, _, _ -> AppResult.Failure(AppError.NetworkFailed("raw private transcript body")) }
        )

        val result = useCase.ask("session-1", "Question?", settings())
        val message = repository.detail("session-1")!!.session.qaHistory.messages.single()

        assertEquals(AppResult.Failure(AppError.NetworkFailed("DeepSeek QA request failed")), result)
        assertEquals(QaMessageStatus.FAILED, message.status)
        assertEquals("DeepSeek QA request failed", message.errorMessage)
        assertFalse(message.errorMessage.orEmpty().contains("private transcript"))
    }

    @Test
    fun retryCreatesNewQuestionFromFailedMessage() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail(transcriptSegments = listOf(transcript("episode content"))))
        var calls = 0
        val useCase = useCase(
            repository = repository,
            answerQuestion = { _, _, _ ->
                calls += 1
                if (calls == 1) {
                    AppResult.Failure(AppError.NetworkFailed("timeout"))
                } else {
                    AppResult.Success("retry answer")
                }
            }
        )

        useCase.ask("session-1", "Question?", settings())
        val failedId = repository.detail("session-1")!!.session.qaHistory.messages.single().id
        val retry = useCase.retry("session-1", failedId, settings())
        val messages = (retry as AppResult.Success).value.qaHistory.messages

        assertEquals(2, messages.size)
        assertEquals(QaMessageStatus.FAILED, messages.first().status)
        assertEquals(QaMessageStatus.ANSWERED, messages.last().status)
        assertEquals("Question?", messages.last().question)
    }

    private fun useCase(
        repository: FakeSessionRepository,
        readApiKey: suspend () -> AppResult<String> = { AppResult.Success("api-key") },
        answerQuestion: suspend (String, String, String) -> AppResult<String> = { _, _, _ ->
            AppResult.Success("answer")
        }
    ): SessionQaUseCase {
        var id = 0
        return SessionQaUseCase(
            sessionRepository = repository,
            readApiKey = readApiKey,
            answerQuestion = answerQuestion,
            nowMillis = { 1_000L + id },
            newId = {
                id += 1
                "qa-$id"
            }
        )
    }

    private class FakeSessionRepository : SessionRepository {
        private val details = MutableStateFlow<Map<String, PodcastSessionDetail>>(emptyMap())

        fun seed(detail: PodcastSessionDetail) {
            details.value = details.value + (detail.session.id to detail)
        }

        fun detail(sessionId: String): PodcastSessionDetail? = details.value[sessionId]

        override suspend fun createSession(title: String?, sourceType: AudioSourceType?): PodcastSession {
            error("Not needed")
        }

        override suspend fun renameSession(sessionId: String, title: String): AppResult<PodcastSession> {
            error("Not needed")
        }

        override suspend fun appendSegment(
            sessionId: String,
            sourceType: AudioSourceType,
            startedAt: Long
        ): AppResult<RecordingSegment> {
            error("Not needed")
        }

        override suspend fun updateSegment(segment: RecordingSegment): AppResult<RecordingSegment> {
            error("Not needed")
        }

        override suspend fun updateStatus(
            sessionId: String,
            status: PodcastSessionStatus,
            errorMessage: String?
        ): AppResult<PodcastSession> {
            error("Not needed")
        }

        override suspend fun updateQaHistory(
            sessionId: String,
            qaHistory: SessionQaHistory
        ): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return AppResult.Failure(AppError.Unknown("missing"))
            val updated = detail.session.copy(qaHistory = qaHistory)
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override fun observeSessions(): Flow<List<PodcastSession>> {
            return details.map { value -> value.values.map { it.session } }
        }

        override fun observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?> {
            return details.map { value -> value[sessionId] }
        }
    }

    private companion object {
        fun settings(): AppSettings = AppSettings(deepSeekModel = "deepseek-chat")

        fun detail(
            transcriptSegments: List<TranscriptSegmentEntity> = emptyList()
        ): PodcastSessionDetail {
            return PodcastSessionDetail(
                session = PodcastSession(
                    id = "session-1",
                    title = "Episode",
                    createdAt = 1L,
                    updatedAt = 2L,
                    sourceType = AudioSourceType.MICROPHONE,
                    status = PodcastSessionStatus.SUMMARIZED,
                    activeSegmentId = null,
                    lastCompletedSegmentId = null,
                    transcript = "",
                    summary = null,
                    summaryStyle = SummaryStyle.BULLET_SUMMARY,
                    summaryLanguage = SummaryLanguage.CHINESE,
                    summaryModelName = "deepseek-chat",
                    asrModelName = "SenseVoice",
                    vadModelName = "VAD",
                    diarizationModelName = "Diarization",
                    detectedSpeakerCount = 0,
                    recordingSegmentCount = 0,
                    transcriptSegmentCount = transcriptSegments.size,
                    errorMessage = null,
                    legacyRecordingSessionId = null
                ),
                recordingSegments = emptyList(),
                transcriptSegments = transcriptSegments,
                speakerProfiles = emptyList()
            )
        }

        fun transcript(text: String): TranscriptSegmentEntity {
            return TranscriptSegmentEntity(
                id = "t-1",
                sessionId = "session-1",
                recordingSegmentId = null,
                startMs = 1_000L,
                endMs = 2_000L,
                speakerId = "speaker-1",
                speakerDisplayName = "Speaker 1",
                text = text,
                language = null,
                confidence = null,
                vadConfidence = null,
                isFinal = true,
                createdAt = 1_000L
            )
        }
    }
}
