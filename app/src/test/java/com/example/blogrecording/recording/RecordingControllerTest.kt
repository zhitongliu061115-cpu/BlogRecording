package com.example.blogrecording.recording

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SessionRepository
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import java.util.Collections
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingControllerTest {
    @Test
    fun fakeRecorderAndRepositoryStartAndPauseOneSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(
            sessionRepository = repository,
            recorder = recorder,
            nowMillis = TickClock()
        )

        val started = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
        val sessionId = (started as AppResult.Success).value.activeSessionId!!
        val segmentId = started.value.activeSegmentId!!
        val paused = controller.pause(sessionId)
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertEquals(1, recorder.starts.size)
        assertEquals(1, recorder.stops.size)
        assertEquals(sessionId, recorder.starts.single().sessionId)
        assertEquals(segmentId, recorder.stops.single().segmentId)
        assertFalse((paused as AppResult.Success).value.isRecording)
        assertEquals(PodcastSessionStatus.PAUSED, detail.session.status)
        assertEquals(RecordingSegmentStatus.COMPLETED, detail.recordingSegments.single().status)
    }

    @Test
    fun startMicrophoneMapsLegacySingleRecordingToOneSessionAndSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        val started = controller.startMicrophone(title = "Mic Episode") as AppResult.Success
        val sessionId = started.value.activeSessionId!!
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertEquals(AudioSourceType.MICROPHONE, started.value.sourceType)
        assertEquals("Mic Episode", detail.session.title)
        assertEquals(PodcastSessionStatus.RECORDING, detail.session.status)
        assertEquals(1, detail.recordingSegments.size)
        assertEquals(AudioSourceType.MICROPHONE, detail.recordingSegments.single().sourceType)
    }

    @Test
    fun pauseAndResumeMicrophoneCreateSegmentBoundariesWithoutFinalizingSession() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())
        val started = controller.startMicrophone(title = "Mic Episode") as AppResult.Success
        val sessionId = started.value.activeSessionId!!

        val paused = controller.pauseMicrophone(sessionId) as AppResult.Success
        val resumed = controller.resumeMicrophone(sessionId) as AppResult.Success
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertFalse(paused.value.isRecording)
        assertTrue(resumed.value.isRecording)
        assertEquals(PodcastSessionStatus.RECORDING, detail.session.status)
        assertEquals(listOf(RecordingSegmentStatus.COMPLETED, RecordingSegmentStatus.RECORDING), detail.recordingSegments.map { it.status })
        assertEquals("segment-2", resumed.value.activeSegmentId)
    }

    @Test
    fun pauseMicrophoneRejectsActiveNonMicrophoneSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        controller.start(title = "System Episode", sourceType = AudioSourceType.INTERNAL_AUDIO)
        val result = controller.pauseMicrophone()

        assertTrue(result is AppResult.Failure)
        assertTrue(controller.currentState().isRecording)
        assertEquals(0, recorder.stops.size)
    }

    @Test
    fun resumePausedSessionAppendsNewSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())
        val firstStart = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
            as AppResult.Success
        val sessionId = firstStart.value.activeSessionId!!

        controller.pause(sessionId)
        val secondStart = controller.resume(sessionId, AudioSourceType.MICROPHONE) as AppResult.Success
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertTrue(secondStart.value.isRecording)
        assertEquals(sessionId, secondStart.value.activeSessionId)
        assertEquals(listOf("segment-1", "segment-2"), detail.recordingSegments.map { it.id })
        assertEquals("segment-2", secondStart.value.activeSegmentId)
        assertEquals(2, recorder.starts.size)
    }

    @Test
    fun switchSessionPausesCurrentBeforeStartingTarget() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())
        val episodeA = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
            as AppResult.Success
        val sessionA = episodeA.value.activeSessionId!!
        val sessionB = repository.createSession(title = "Episode B", sourceType = AudioSourceType.INTERNAL_AUDIO)

        val switched = controller.switchSession(sessionB.id, AudioSourceType.INTERNAL_AUDIO)
            as AppResult.Success
        val detailA = repository.observeSessionDetail(sessionA).first()!!
        val detailB = repository.observeSessionDetail(sessionB.id).first()!!

        assertEquals(listOf("start:session-1:segment-1", "stop:session-1:segment-1", "start:session-2:segment-2"), recorder.events)
        assertEquals(PodcastSessionStatus.PAUSED, detailA.session.status)
        assertEquals(RecordingSegmentStatus.COMPLETED, detailA.recordingSegments.single().status)
        assertEquals(PodcastSessionStatus.RECORDING, detailB.session.status)
        assertEquals(sessionB.id, switched.value.activeSessionId)
        assertEquals("segment-2", switched.value.activeSegmentId)
    }

    @Test
    fun finalizeRecordingSessionPausesSegmentThenMarksReadyForSummary() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())
        val started = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
            as AppResult.Success
        val sessionId = started.value.activeSessionId!!

        val finalized = controller.finalize(sessionId)
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertFalse((finalized as AppResult.Success).value.isRecording)
        assertEquals(listOf("start:session-1:segment-1", "stop:session-1:segment-1"), recorder.events)
        assertEquals(PodcastSessionStatus.READY_FOR_SUMMARY, detail.session.status)
        assertNull(detail.session.activeSegmentId)
        assertEquals(RecordingSegmentStatus.COMPLETED, detail.recordingSegments.single().status)
    }

    @Test
    fun resumeSameActiveSessionIsIdempotent() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())
        val started = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
            as AppResult.Success
        val sessionId = started.value.activeSessionId!!

        val resumed = controller.resume(sessionId, AudioSourceType.MICROPHONE) as AppResult.Success
        val detail = repository.observeSessionDetail(sessionId).first()!!

        assertEquals(started.value, resumed.value)
        assertEquals(1, recorder.starts.size)
        assertEquals(1, detail.recordingSegments.size)
    }

    @Test
    fun duplicateStartReturnsExistingActiveSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        val first = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
            as AppResult.Success
        val duplicate = controller.start(title = "Episode B", sourceType = AudioSourceType.INTERNAL_AUDIO)
            as AppResult.Success
        val sessions = repository.observeSessions().first()

        assertEquals(first.value, duplicate.value)
        assertEquals(1, sessions.size)
        assertEquals(1, recorder.starts.size)
        assertEquals(AudioSourceType.MICROPHONE, duplicate.value.sourceType)
    }

    @Test
    fun concurrentStartCreatesAtMostOneActiveSegment() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        val results = (1..8).map { index ->
            async {
                controller.start(
                    title = "Episode $index",
                    sourceType = AudioSourceType.MICROPHONE
                )
            }
        }.awaitAll()
        val states = results.map { (it as AppResult.Success).value }
        val sessions = repository.observeSessions().first()
        val details = sessions.map { repository.observeSessionDetail(it.id).first()!! }

        assertEquals(1, states.map { it.activeSegmentId }.distinct().size)
        assertEquals(1, sessions.size)
        assertEquals(1, details.single().recordingSegments.size)
        assertEquals(1, recorder.starts.size)
    }

    @Test
    fun recorderStartFailureDoesNotLeaveControllerActive() = runBlocking {
        val repository = FakeSessionRepository()
        val recorder = FakeSegmentRecorder(startFailure = AppError.AudioRecordInitFailed("init failed"))
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        val result = controller.start(title = "Episode A", sourceType = AudioSourceType.MICROPHONE)
        val detail = repository.observeSessions().first().single().let { session ->
            repository.observeSessionDetail(session.id).first()!!
        }

        assertTrue(result is AppResult.Failure)
        assertFalse(controller.currentState().isRecording)
        assertEquals(PodcastSessionStatus.ERROR, detail.session.status)
        assertEquals(RecordingSegmentStatus.ERROR, detail.recordingSegments.single().status)
        assertEquals("Audio recorder failed to start", detail.recordingSegments.single().errorMessage)
    }

    @Test
    fun recoverInterruptedRecordingClearsActiveSessionAndMarksSegmentInterrupted() = runBlocking {
        val repository = FakeSessionRepository()
        repository.seed(
            session = session(
                id = "session-recovered",
                title = "Recovered",
                sourceType = AudioSourceType.MICROPHONE
            ).copy(
                status = PodcastSessionStatus.RECORDING,
                activeSegmentId = "segment-recovered",
                recordingSegmentCount = 1
            ),
            segments = listOf(
                recordingSegment(
                    id = "segment-recovered",
                    sessionId = "session-recovered",
                    sourceType = AudioSourceType.MICROPHONE,
                    index = 1,
                    startedAt = 100L
                )
            )
        )
        val recorder = FakeSegmentRecorder()
        val controller = RecordingController(repository, recorder, nowMillis = TickClock())

        val result = controller.recoverInterruptedRecordings() as AppResult.Success
        val detail = repository.observeSessionDetail("session-recovered").first()!!

        assertEquals(1, result.value.recoveredSessionCount)
        assertEquals(PodcastSessionStatus.PAUSED, detail.session.status)
        assertNull(detail.session.activeSegmentId)
        assertEquals(RecordingSegmentStatus.INTERRUPTED, detail.recordingSegments.single().status)
        assertEquals(0, recorder.stops.size)
        assertFalse(controller.currentState().isRecording)
    }

    private class FakeSegmentRecorder(
        private val startFailure: AppError? = null,
        private val stopFailure: AppError? = null
    ) : SegmentRecorder {
        val starts = Collections.synchronizedList(mutableListOf<SegmentStartRequest>())
        val stops = Collections.synchronizedList(mutableListOf<ActiveRecordingSegment>())
        val events = Collections.synchronizedList(mutableListOf<String>())

        override suspend fun start(request: SegmentStartRequest): AppResult<Unit> {
            starts += request
            events += "start:${request.sessionId}:${request.segmentId}"
            val failure = startFailure
            return if (failure == null) AppResult.Success(Unit) else AppResult.Failure(failure)
        }

        override suspend fun stop(activeSegment: ActiveRecordingSegment): AppResult<SegmentStopResult> {
            stops += activeSegment
            events += "stop:${activeSegment.sessionId}:${activeSegment.segmentId}"
            val failure = stopFailure
            return if (failure == null) {
                AppResult.Success(SegmentStopResult(endedAt = 10_000L + stops.size, durationMs = 1_000L))
            } else {
                AppResult.Failure(failure)
            }
        }
    }

    private class FakeSessionRepository : SessionRepository {
        private val details = MutableStateFlow<Map<String, PodcastSessionDetail>>(emptyMap())
        private var nextSession = 1
        private var nextSegment = 1

        fun seed(
            session: PodcastSession,
            segments: List<RecordingSegment> = emptyList()
        ) {
            details.value = details.value + (session.id to PodcastSessionDetail(
                session = session,
                recordingSegments = segments,
                transcriptSegments = emptyList(),
                speakerProfiles = emptyList()
            ))
        }

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
            val updated = detail.session.copy(title = title, updatedAt = 999L)
            details.value = details.value + (sessionId to detail.copy(session = updated))
            return AppResult.Success(updated)
        }

        override suspend fun appendSegment(
            sessionId: String,
            sourceType: AudioSourceType,
            startedAt: Long
        ): AppResult<RecordingSegment> {
            val detail = details.value[sessionId] ?: return missing()
            val segmentId = "segment-${nextSegment++}"
            val segment = recordingSegment(
                id = segmentId,
                sessionId = sessionId,
                sourceType = sourceType,
                index = detail.recordingSegments.size + 1,
                startedAt = startedAt
            )
            val updatedSession = detail.session.copy(
                sourceType = sourceType,
                activeSegmentId = segmentId,
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
                    ?: detail.recordingSegments.lastOrNull()?.id
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

    private class TickClock : () -> Long {
        private var value = 100L

        override fun invoke(): Long {
            value += 100L
            return value
        }
    }

    private companion object {
        fun session(
            id: String,
            title: String,
            sourceType: AudioSourceType?
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
            id: String,
            sessionId: String,
            sourceType: AudioSourceType,
            index: Int,
            startedAt: Long
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
    }
}
