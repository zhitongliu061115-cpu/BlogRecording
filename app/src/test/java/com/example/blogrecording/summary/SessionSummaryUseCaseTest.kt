package com.example.blogrecording.summary

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SessionHighlights
import com.example.blogrecording.data.SessionRepository
import com.example.blogrecording.data.SessionTagGeneration
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.StructuredSummary
import com.example.blogrecording.data.StructuredSummaryParseStatus
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TagGenerationStatus
import com.example.blogrecording.data.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryUseCaseTest {
    @Test
    fun startsSummaryWithAggregatedSessionTranscript() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(
            detail(
                recordingSegments = listOf(segment(id = "segment-2", index = 2), segment(id = "segment-1", index = 1)),
                transcriptSegments = listOf(
                    transcript(id = "t-2", recordingSegmentId = "segment-2", startMs = 200L, text = "second segment"),
                    transcript(id = "t-1", recordingSegmentId = "segment-1", startMs = 100L, text = "first segment")
                )
            )
        )
        var capturedTranscript = ""
        val useCase = useCase(
            repository = repository,
            generateSummary = { _, transcript, _ ->
                capturedTranscript = transcript
                AppResult.Success(summaryResult("session summary"))
            }
        )

        val result = useCase.start("session-1", settings())
        val session = (result as AppResult.Success).value

        assertEquals(PodcastSessionStatus.SUMMARIZED, session.status)
        assertEquals(SummaryStatus.SUMMARIZED, session.summary?.status)
        assertEquals("session summary", session.summary?.text)
        assertEquals("session summary", session.summary?.structured?.overview)
        assertEquals(TagGenerationStatus.GENERATED, session.tagGeneration.status)
        assertEquals(listOf("session summary"), session.tagGeneration.tags.map { it.text })
        assertEquals(listOf("first segment", "second segment"), session.highlights.items.map { it.text })
        assertEquals(listOf("t-1"), session.highlights.items.first().transcriptSegmentIds)
        assertOrdered(capturedTranscript, "first segment", "second segment")
    }

    @Test
    fun missingApiKeyDoesNotSendSummaryRequestAndMarksFailed() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(detail(transcriptSegments = listOf(transcript(text = "ready"))))
        var generateCalls = 0
        val useCase = useCase(
            repository = repository,
            readApiKey = { AppResult.Failure(AppError.DeepSeekApiKeyMissing) },
            generateSummary = { _, _, _ ->
                generateCalls += 1
                AppResult.Success(summaryResult("should not happen"))
            }
        )

        val result = useCase.start("session-1", settings())
        val latest = repository.detail("session-1")!!.session

        assertEquals(AppResult.Failure(AppError.DeepSeekApiKeyMissing), result)
        assertEquals(0, generateCalls)
        assertEquals(SummaryStatus.FAILED, latest.summary?.status)
        assertEquals("DeepSeek API Key missing", latest.summary?.errorMessage)
        assertEquals(TagGenerationStatus.BLOCKED_MISSING_API_KEY, latest.tagGeneration.status)
    }

    @Test
    fun providerFailureMarksFailedAndPreservesPreviousSummary() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(
            detail(
                session = session(
                    status = PodcastSessionStatus.SUMMARIZED,
                    summary = summary(
                        text = "previous summary",
                        status = SummaryStatus.SUMMARIZED,
                        generatedAt = 500L
                    )
                ),
                transcriptSegments = listOf(transcript(text = "ready"))
            )
        )
        val useCase = useCase(
            repository = repository,
            generateSummary = { _, _, _ ->
                AppResult.Failure(AppError.NetworkFailed("provider raw payload with transcript ready"))
            }
        )

        val result = useCase.start("session-1", settings())
        val latest = repository.detail("session-1")!!.session

        assertEquals(AppResult.Failure(AppError.NetworkFailed("DeepSeek summary request failed")), result)
        assertEquals(PodcastSessionStatus.READY_FOR_SUMMARY, latest.status)
        assertEquals(SummaryStatus.FAILED, latest.summary?.status)
        assertEquals("previous summary", latest.summary?.text)
        assertEquals("previous summary", latest.summary?.structured?.overview)
        assertEquals(500L, latest.summary?.generatedAt)
        assertEquals("DeepSeek summary request failed", latest.summary?.errorMessage)
        assertEquals(TagGenerationStatus.FAILED, latest.tagGeneration.status)
        assertFalse(latest.summary?.errorMessage.orEmpty().contains("ready"))
    }

    @Test
    fun activeRecordingAndMissingTranscriptBlockSummaryBeforeApiKeyRead() = runBlocking {
        val recordingRepository = FakeSessionRepository()
        recordingRepository.seed(
            detail(
                session = session(
                    status = PodcastSessionStatus.RECORDING,
                    activeSegmentId = "segment-1"
                ),
                recordingSegments = listOf(segment(id = "segment-1", status = RecordingSegmentStatus.RECORDING)),
                transcriptSegments = listOf(transcript(text = "ready"))
            )
        )
        val blankRepository = FakeSessionRepository()
        blankRepository.seed(detail())
        var apiKeyReads = 0
        val useCaseRecording = useCase(recordingRepository, readApiKey = {
            apiKeyReads += 1
            AppResult.Success("key")
        })
        val useCaseBlank = useCase(blankRepository, readApiKey = {
            apiKeyReads += 1
            AppResult.Success("key")
        })

        val recordingResult = useCaseRecording.start("session-1", settings())
        val blankResult = useCaseBlank.start("session-1", settings())

        assertTrue(recordingResult is AppResult.Failure)
        assertTrue(blankResult is AppResult.Failure)
        assertEquals(0, apiKeyReads)
        assertEquals(TagGenerationStatus.BLOCKED_EMPTY_CONTENT, blankRepository.detail("session-1")?.session?.tagGeneration?.status)
    }

    private fun useCase(
        repository: FakeSessionRepository,
        readApiKey: suspend () -> AppResult<String> = { AppResult.Success("api-key") },
        generateSummary: suspend (String, String, AppSettings) -> AppResult<SummaryGenerationResult> = { _, _, _ ->
            AppResult.Success(summaryResult("summary"))
        }
    ): SessionSummaryUseCase {
        return SessionSummaryUseCase(
            sessionRepository = repository,
            readApiKey = readApiKey,
            generateSummary = generateSummary,
            nowMillis = { 1_000L }
        )
    }

    private fun assertOrdered(text: String, vararg parts: String) {
        var previous = -1
        parts.forEach { part ->
            val index = text.indexOf(part)
            assertTrue("$part was not found in $text", index >= 0)
            assertTrue("$part was not ordered after previous part", index > previous)
            previous = index
        }
    }

    private class FakeSessionRepository : SessionRepository {
        private val details = MutableStateFlow<Map<String, PodcastSessionDetail>>(emptyMap())

        fun seed(detail: PodcastSessionDetail) {
            details.value = details.value + (detail.session.id to detail)
        }

        fun detail(sessionId: String): PodcastSessionDetail? {
            return details.value[sessionId]
        }

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

        override suspend fun updateSummaryLifecycle(
            sessionId: String,
            status: SummaryStatus,
            modelName: String,
            summaryText: String?,
            generatedAt: Long?,
            structuredSummary: StructuredSummary?,
            tagGeneration: SessionTagGeneration?,
            highlights: SessionHighlights?,
            errorMessage: String?
        ): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return AppResult.Failure(AppError.Unknown("missing"))
            val existing = detail.session.summary
            val summary = SessionSummary(
                text = when (status) {
                    SummaryStatus.SUMMARIZED -> summaryText.orEmpty()
                    SummaryStatus.NOT_READY,
                    SummaryStatus.READY,
                    SummaryStatus.SUMMARIZING,
                    SummaryStatus.FAILED -> existing?.text.orEmpty()
                },
                status = status,
                modelName = modelName,
                generatedAt = when (status) {
                    SummaryStatus.SUMMARIZED -> generatedAt
                    SummaryStatus.NOT_READY,
                    SummaryStatus.READY,
                    SummaryStatus.SUMMARIZING,
                    SummaryStatus.FAILED -> existing?.generatedAt
                },
                updatedAt = 1_000L,
                errorMessage = if (status == SummaryStatus.FAILED) errorMessage?.take(160) else null,
                structured = when (status) {
                    SummaryStatus.SUMMARIZED -> structuredSummary
                    SummaryStatus.NOT_READY,
                    SummaryStatus.READY,
                    SummaryStatus.SUMMARIZING,
                    SummaryStatus.FAILED -> existing?.structured
                }
            )
            val sessionStatus = when (status) {
                SummaryStatus.NOT_READY -> detail.session.status
                SummaryStatus.READY -> PodcastSessionStatus.READY_FOR_SUMMARY
                SummaryStatus.SUMMARIZING -> PodcastSessionStatus.SUMMARIZING
                SummaryStatus.SUMMARIZED -> PodcastSessionStatus.SUMMARIZED
                SummaryStatus.FAILED -> PodcastSessionStatus.READY_FOR_SUMMARY
            }
            val updated = detail.session.copy(
                status = sessionStatus,
                activeSegmentId = null,
                summary = summary,
                summaryModelName = modelName,
                tagGeneration = if (status == SummaryStatus.SUMMARIZED) {
                    tagGeneration ?: detail.session.tagGeneration
                } else {
                    detail.session.tagGeneration
                },
                highlights = if (status == SummaryStatus.SUMMARIZED) {
                    highlights ?: detail.session.highlights
                } else {
                    detail.session.highlights
                },
                errorMessage = if (status == SummaryStatus.FAILED) summary.errorMessage else null
            )
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override suspend fun updateTagGeneration(
            sessionId: String,
            tagGeneration: SessionTagGeneration
        ): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return AppResult.Failure(AppError.Unknown("missing"))
            val updated = detail.session.copy(tagGeneration = tagGeneration)
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override suspend fun updateHighlights(
            sessionId: String,
            highlights: SessionHighlights
        ): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return AppResult.Failure(AppError.Unknown("missing"))
            val updated = detail.session.copy(highlights = highlights)
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
        fun settings(): AppSettings {
            return AppSettings(deepSeekModel = "deepseek-chat")
        }

        fun detail(
            session: PodcastSession = session(),
            recordingSegments: List<RecordingSegment> = emptyList(),
            transcriptSegments: List<TranscriptSegmentEntity> = emptyList()
        ): PodcastSessionDetail {
            return PodcastSessionDetail(
                session = session,
                recordingSegments = recordingSegments,
                transcriptSegments = transcriptSegments,
                speakerProfiles = emptyList()
            )
        }

        fun session(
            status: PodcastSessionStatus = PodcastSessionStatus.READY_FOR_SUMMARY,
            activeSegmentId: String? = null,
            summary: SessionSummary? = null
        ): PodcastSession {
            return PodcastSession(
                id = "session-1",
                title = "Episode",
                createdAt = 1L,
                updatedAt = 2L,
                sourceType = AudioSourceType.MICROPHONE,
                status = status,
                activeSegmentId = activeSegmentId,
                lastCompletedSegmentId = null,
                transcript = "",
                summary = summary,
                summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
                summaryLanguage = SummaryLanguage.CHINESE,
                summaryModelName = "deepseek-chat",
                asrModelName = "SenseVoice sherpa-onnx",
                vadModelName = "Silero VAD sherpa-onnx",
                diarizationModelName = "sherpa-onnx speaker diarization",
                detectedSpeakerCount = 0,
                recordingSegmentCount = 0,
                transcriptSegmentCount = 0,
                errorMessage = null,
                legacyRecordingSessionId = null
            )
        }

        fun segment(
            id: String,
            index: Int = 1,
            status: RecordingSegmentStatus = RecordingSegmentStatus.COMPLETED
        ): RecordingSegment {
            return RecordingSegment(
                id = id,
                sessionId = "session-1",
                index = index,
                sourceType = AudioSourceType.MICROPHONE,
                status = status,
                startedAt = index * 1_000L,
                endedAt = index * 1_000L + 500L,
                durationMs = 500L,
                pcmFilePath = null,
                audioFilePath = null,
                sampleRate = null,
                channelCount = null,
                transcriptSegmentIds = emptyList(),
                errorMessage = null,
                createdAt = index * 1_000L,
                updatedAt = index * 1_000L + 500L
            )
        }

        fun transcript(
            id: String = "transcript-1",
            recordingSegmentId: String? = null,
            startMs: Long = 100L,
            text: String
        ): TranscriptSegmentEntity {
            return TranscriptSegmentEntity(
                id = id,
                sessionId = "session-1",
                recordingSegmentId = recordingSegmentId,
                startMs = startMs,
                endMs = startMs + 500L,
                speakerId = "speaker-1",
                speakerDisplayName = "Speaker 1",
                text = text,
                language = "zh",
                confidence = null,
                vadConfidence = null,
                isFinal = true,
                createdAt = startMs
            )
        }

        fun summary(
            text: String,
            status: SummaryStatus,
            generatedAt: Long?
        ): SessionSummary {
            return SessionSummary(
                text = text,
                status = status,
                modelName = "deepseek-chat",
                generatedAt = generatedAt,
                updatedAt = generatedAt ?: 1L,
                errorMessage = null,
                structured = StructuredSummary(
                    overview = text,
                    keyPoints = emptyList(),
                    actionItems = emptyList(),
                    openQuestions = emptyList(),
                    quoteCandidates = emptyList(),
                    parseStatus = StructuredSummaryParseStatus.FALLBACK_TEXT
                )
            )
        }

        fun summaryResult(text: String): SummaryGenerationResult {
            return SummaryGenerationResult(
                text = text,
                structured = StructuredSummary(
                    overview = text,
                    keyPoints = emptyList(),
                    actionItems = emptyList(),
                    openQuestions = emptyList(),
                    quoteCandidates = emptyList(),
                    parseStatus = StructuredSummaryParseStatus.FALLBACK_TEXT
                ),
                tagGeneration = SessionTagGenerator.generate(
                    rawModelText = """{"tags":["$text"]}""",
                    structured = null,
                    transcript = text,
                    generatedAt = 1_000L
                ),
                highlights = SessionHighlightGenerator.generate(
                    structured = StructuredSummary(
                        overview = text,
                        keyPoints = emptyList(),
                        actionItems = emptyList(),
                        openQuestions = emptyList(),
                        quoteCandidates = listOf("quote"),
                        parseStatus = StructuredSummaryParseStatus.STRUCTURED
                    ),
                    transcriptSegments = emptyList(),
                    fallbackTranscript = text,
                    generatedAt = 1_000L
                )
            )
        }
    }
}
