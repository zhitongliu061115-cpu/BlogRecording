package com.example.blogrecording.data

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryContractTest {
    @Test
    fun repositorySurfaceSupportsCreateRenameSegmentStatusAndDetailObservation() = runBlocking {
        val repository = FakeSessionRepository()

        val created = repository.createSession(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
        val renamed = repository.renameSession(created.id, "Episode B")
        val segment = repository.appendSegment(created.id, AudioSourceType.MICROPHONE, startedAt = 100L)
        val recording = repository.updateStatus(created.id, PodcastSessionStatus.RECORDING)
        val detail = repository.observeSessionDetail(created.id).first()

        assertEquals("Episode A", created.title)
        assertEquals("Episode B", (renamed as AppResult.Success).value.title)
        assertEquals("segment-1", (segment as AppResult.Success).value.id)
        assertEquals(PodcastSessionStatus.RECORDING, (recording as AppResult.Success).value.status)
        assertEquals(created.id, detail?.session?.id)
        assertEquals(1, detail?.recordingSegments?.size)
    }

    @Test
    fun podcastSessionDetailAggregatesOnlyOneSessionData() {
        val session = session(id = "session-1")
        val detail = PodcastSessionDetail(
            session = session,
            recordingSegments = listOf(recordingSegment(sessionId = "session-1")),
            transcriptSegments = listOf(transcriptSegment(sessionId = "session-1")),
            speakerProfiles = listOf(speakerProfile(sessionId = "session-1"))
        )

        assertEquals("session-1", detail.session.id)
        assertTrue(detail.recordingSegments.all { it.sessionId == "session-1" })
        assertTrue(detail.transcriptSegments.all { it.sessionId == "session-1" })
        assertTrue(detail.speakerProfiles.all { it.sessionId == "session-1" })
    }

    @Test
    fun missingDetailEmitsNull() = runBlocking {
        val repository = FakeSessionRepository()

        val detail = repository.observeSessionDetail("missing").first()

        assertNull(detail)
    }

    @Test
    fun observeSessionsIncludesMultipleCreatedSessions() = runBlocking {
        val repository = FakeSessionRepository()

        val first = repository.createSession(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
        val second = repository.createSession(title = "Episode B", sourceType = AudioSourceType.INTERNAL_AUDIO)
        val sessions = repository.observeSessions().first()

        assertEquals(listOf(first.id, second.id), sessions.map { it.id })
        assertEquals(listOf("Episode A", "Episode B"), sessions.map { it.title })
    }

    @Test
    fun updateSegmentReplacesMatchingRecordingSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val created = repository.createSession(title = "Episode", sourceType = AudioSourceType.MICROPHONE)
        val segment = (repository.appendSegment(created.id, AudioSourceType.MICROPHONE, startedAt = 100L) as AppResult.Success).value
        val completed = segment.copy(status = RecordingSegmentStatus.COMPLETED, endedAt = 200L, durationMs = 100L)

        val result = repository.updateSegment(completed)
        val detail = repository.observeSessionDetail(created.id).first()

        assertEquals(completed, (result as AppResult.Success).value)
        assertEquals(RecordingSegmentStatus.COMPLETED, detail?.recordingSegments?.single()?.status)
        assertEquals(100L, detail?.recordingSegments?.single()?.durationMs)
    }

    @Test
    fun repositoryPersistsActiveSegmentAndClearsItWhenPaused() = runBlocking {
        val repository = FakeSessionRepository()
        val created = repository.createSession(title = "Episode", sourceType = AudioSourceType.MICROPHONE)
        val segment = (repository.appendSegment(created.id, AudioSourceType.MICROPHONE, startedAt = 100L) as AppResult.Success).value

        val recording = repository.updateStatus(created.id, PodcastSessionStatus.RECORDING) as AppResult.Success
        val completed = repository.updateSegment(
            segment.copy(status = RecordingSegmentStatus.COMPLETED, endedAt = 200L, durationMs = 100L)
        )
        val paused = repository.updateStatus(created.id, PodcastSessionStatus.PAUSED) as AppResult.Success
        val detail = repository.observeSessionDetail(created.id).first()

        assertEquals(segment.id, recording.value.activeSegmentId)
        assertTrue(completed is AppResult.Success)
        assertNull(paused.value.activeSegmentId)
        assertEquals(segment.id, paused.value.lastCompletedSegmentId)
        assertEquals(RecordingSegmentStatus.COMPLETED, detail?.recordingSegments?.single()?.status)
    }

    @Test
    fun repositoryPersistsInterruptedAndErrorSegmentStates() = runBlocking {
        val repository = FakeSessionRepository()
        val created = repository.createSession(title = "Episode", sourceType = AudioSourceType.MICROPHONE)
        val first = (repository.appendSegment(created.id, AudioSourceType.MICROPHONE, startedAt = 100L) as AppResult.Success).value
        val interrupted = first.copy(
            status = RecordingSegmentStatus.INTERRUPTED,
            endedAt = 150L,
            durationMs = 50L,
            errorMessage = "interrupted"
        )
        repository.updateSegment(interrupted)
        val second = (repository.appendSegment(created.id, AudioSourceType.MICROPHONE, startedAt = 200L) as AppResult.Success).value
        val failed = second.copy(status = RecordingSegmentStatus.ERROR, errorMessage = "failed")

        repository.updateSegment(failed)
        repository.updateStatus(created.id, PodcastSessionStatus.ERROR, errorMessage = "failed")
        val detail = repository.observeSessionDetail(created.id).first()

        assertEquals(listOf(RecordingSegmentStatus.INTERRUPTED, RecordingSegmentStatus.ERROR), detail?.recordingSegments?.map { it.status })
        assertEquals(PodcastSessionStatus.ERROR, detail?.session?.status)
        assertNull(detail?.session?.activeSegmentId)
    }

    @Test
    fun missingSessionMutationsReturnFailure() = runBlocking {
        val repository = FakeSessionRepository()

        assertTrue(repository.renameSession("missing", "title") is AppResult.Failure)
        assertTrue(repository.appendSegment("missing", AudioSourceType.MICROPHONE, startedAt = 100L) is AppResult.Failure)
        assertTrue(repository.updateStatus("missing", PodcastSessionStatus.ERROR) is AppResult.Failure)
        assertTrue(repository.updateSegment(recordingSegment(sessionId = "missing")) is AppResult.Failure)
    }

    private class FakeSessionRepository : SessionRepository {
        private val details = MutableStateFlow<Map<String, PodcastSessionDetail>>(emptyMap())
        private var nextSession = 1
        private var nextSegment = 1

        override suspend fun createSession(
            title: String?,
            sourceType: AudioSourceType?
        ): PodcastSession {
            val session = session(
                id = "session-${nextSession++}",
                title = title ?: "Untitled",
                sourceType = sourceType
            )
            details.value = details.value + (session.id to PodcastSessionDetail(
                session = session,
                recordingSegments = emptyList(),
                transcriptSegments = emptyList(),
                speakerProfiles = emptyList()
            ))
            return session
        }

        override suspend fun renameSession(sessionId: String, title: String): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return missing()
            val updated = detail.session.copy(title = title)
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override suspend fun appendSegment(
            sessionId: String,
            sourceType: AudioSourceType,
            startedAt: Long
        ): AppResult<RecordingSegment> {
            val detail = details.value[sessionId] ?: return missing()
            val segment = recordingSegment(
                id = "segment-${nextSegment++}",
                sessionId = sessionId,
                sourceType = sourceType,
                startedAt = startedAt,
                index = detail.recordingSegments.size + 1
            )
            val updatedSession = detail.session.copy(
                sourceType = sourceType,
                activeSegmentId = segment.id,
                recordingSegmentCount = detail.recordingSegments.size + 1
            )
            details.value = details.value + (sessionId to detail.copy(
                session = updatedSession,
                recordingSegments = detail.recordingSegments + segment
            ))
            return AppResult.Success(segment)
        }

        override suspend fun updateSegment(segment: RecordingSegment): AppResult<RecordingSegment> {
            val detail = details.value[segment.sessionId] ?: return missing()
            if (detail.recordingSegments.none { it.id == segment.id }) return missing()
            details.value = details.value + (segment.sessionId to detail.copy(
                recordingSegments = detail.recordingSegments.map {
                    if (it.id == segment.id) segment else it
                }
            ))
            return AppResult.Success(segment)
        }

        override suspend fun updateStatus(
            sessionId: String,
            status: PodcastSessionStatus,
            errorMessage: String?
        ): AppResult<PodcastSession> {
            val detail = details.value[sessionId] ?: return missing()
            val activeSegmentId = when (status) {
                PodcastSessionStatus.RECORDING -> detail.session.activeSegmentId
                    ?: detail.recordingSegments.lastOrNull { it.status == RecordingSegmentStatus.RECORDING }?.id
                PodcastSessionStatus.PAUSED,
                PodcastSessionStatus.READY_FOR_SUMMARY,
                PodcastSessionStatus.ERROR -> null
                PodcastSessionStatus.DRAFT,
                PodcastSessionStatus.PROCESSING,
                PodcastSessionStatus.SUMMARIZING,
                PodcastSessionStatus.SUMMARIZED -> detail.session.activeSegmentId
            }
            val lastCompletedSegmentId = if (status == PodcastSessionStatus.PAUSED) {
                detail.recordingSegments.lastOrNull { it.status == RecordingSegmentStatus.COMPLETED }?.id
            } else {
                detail.session.lastCompletedSegmentId
            }
            val updated = detail.session.copy(
                status = status,
                activeSegmentId = activeSegmentId,
                lastCompletedSegmentId = lastCompletedSegmentId,
                errorMessage = errorMessage
            )
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override fun observeSessions(): Flow<List<PodcastSession>> {
            return details.map { value -> value.values.map { it.session } }
        }

        override fun observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?> {
            return details.map { value -> value[sessionId] }
        }

        private fun <T> missing(): AppResult<T> {
            return AppResult.Failure(AppError.Unknown("missing session"))
        }
    }

    private companion object {
        fun session(
            id: String = "session-1",
            title: String = "Episode",
            sourceType: AudioSourceType? = AudioSourceType.MICROPHONE
        ): PodcastSession {
            return PodcastSession(
                id = id,
                title = title,
                createdAt = 1L,
                updatedAt = 2L,
                sourceType = sourceType,
                status = PodcastSessionStatus.DRAFT,
                activeSegmentId = null,
                lastCompletedSegmentId = null,
                transcript = "",
                summary = null,
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

        fun recordingSegment(
            id: String = "segment-1",
            sessionId: String,
            sourceType: AudioSourceType = AudioSourceType.MICROPHONE,
            startedAt: Long = 100L,
            index: Int = 1
        ): RecordingSegment {
            return RecordingSegment(
                id = id,
                sessionId = sessionId,
                index = index,
                sourceType = sourceType,
                status = RecordingSegmentStatus.RECORDING,
                startedAt = startedAt,
                endedAt = null,
                durationMs = 0L,
                pcmFilePath = null,
                audioFilePath = null,
                sampleRate = null,
                channelCount = null,
                transcriptSegmentIds = emptyList(),
                errorMessage = null,
                createdAt = startedAt,
                updatedAt = startedAt
            )
        }

        fun transcriptSegment(sessionId: String): TranscriptSegmentEntity {
            return TranscriptSegmentEntity(
                id = "transcript-1",
                sessionId = sessionId,
                startMs = 0L,
                endMs = 1_000L,
                speakerId = "speaker_1",
                speakerDisplayName = "Speaker 1",
                text = "hello",
                language = "zh",
                confidence = null,
                vadConfidence = null,
                isFinal = true,
                createdAt = 100L
            )
        }

        fun speakerProfile(sessionId: String): SpeakerProfileEntity {
            return SpeakerProfileEntity(
                id = "profile-1",
                sessionId = sessionId,
                speakerId = "speaker_1",
                displayName = "Speaker 1",
                colorIndex = 0,
                segmentCount = 1,
                totalSpeechDurationMs = 1_000L,
                createdAt = 100L,
                updatedAt = 100L
            )
        }
    }
}
