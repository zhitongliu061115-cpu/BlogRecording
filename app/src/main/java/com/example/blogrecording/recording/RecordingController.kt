package com.example.blogrecording.recording

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActiveRecordingSegment(
    val sessionId: String,
    val segmentId: String,
    val sourceType: AudioSourceType
)

data class SegmentStartRequest(
    val sessionId: String,
    val segmentId: String,
    val sourceType: AudioSourceType
)

data class SegmentStopResult(
    val endedAt: Long,
    val durationMs: Long
)

data class RecordingControllerState(
    val activeSessionId: String?,
    val activeSegmentId: String?,
    val sourceType: AudioSourceType?
) {
    val isRecording: Boolean = activeSessionId != null && activeSegmentId != null

    companion object {
        val Idle = RecordingControllerState(
            activeSessionId = null,
            activeSegmentId = null,
            sourceType = null
        )
    }
}

data class RecordingRecoveryResult(
    val recoveredSessionCount: Int
)

interface SegmentRecorder {
    suspend fun start(request: SegmentStartRequest): AppResult<Unit>

    suspend fun stop(activeSegment: ActiveRecordingSegment): AppResult<SegmentStopResult>
}

class RecordingController(
    private val sessionRepository: SessionRepository,
    private val recorder: SegmentRecorder,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()
    private var activeSegment: ActiveRecordingSegment? = null

    suspend fun start(
        title: String? = null,
        sourceType: AudioSourceType
    ): AppResult<RecordingControllerState> {
        return mutex.withLock {
            activeSegment?.let { return@withLock AppResult.Success(toState(it)) }
            val session = sessionRepository.createSession(title = title, sourceType = sourceType)
            startSegmentLocked(session.id, sourceType)
        }
    }

    suspend fun resume(
        sessionId: String,
        sourceType: AudioSourceType
    ): AppResult<RecordingControllerState> {
        return mutex.withLock {
            val active = activeSegment
            if (active?.sessionId == sessionId) {
                return@withLock if (active.sourceType == sourceType) {
                    AppResult.Success(toState(active))
                } else {
                    AppResult.Failure(
                        AppError.RecordingPipelineFailed("Session is already recording with another source")
                    )
                }
            }

            val pauseResult = pauseActiveBeforeSwitchLocked()
            if (pauseResult is AppResult.Failure) return@withLock pauseResult
            startSegmentLocked(sessionId, sourceType)
        }
    }

    suspend fun switchSession(
        sessionId: String,
        sourceType: AudioSourceType
    ): AppResult<RecordingControllerState> {
        return resume(sessionId = sessionId, sourceType = sourceType)
    }

    suspend fun pause(sessionId: String? = null): AppResult<RecordingControllerState> {
        return mutex.withLock {
            val active = activeSegment ?: return@withLock AppResult.Success(RecordingControllerState.Idle)
            if (sessionId != null && active.sessionId != sessionId) {
                return@withLock AppResult.Failure(
                    AppError.RecordingPipelineFailed("Active recording belongs to another session")
                )
            }
            pauseActiveLocked(active)
        }
    }

    suspend fun finalize(sessionId: String): AppResult<RecordingControllerState> {
        return mutex.withLock {
            val active = activeSegment
            if (active != null) {
                if (active.sessionId != sessionId) {
                    return@withLock AppResult.Failure(
                        AppError.RecordingPipelineFailed("Another session is actively recording")
                    )
                }
                val pauseResult = pauseActiveLocked(active)
                if (pauseResult is AppResult.Failure) return@withLock pauseResult
            }

            when (val result = sessionRepository.updateStatus(
                sessionId = sessionId,
                status = PodcastSessionStatus.READY_FOR_SUMMARY
            )) {
                is AppResult.Success -> AppResult.Success(RecordingControllerState.Idle)
                is AppResult.Failure -> result
            }
        }
    }

    fun currentState(): RecordingControllerState {
        return activeSegment?.let(::toState) ?: RecordingControllerState.Idle
    }

    suspend fun recoverInterruptedRecordings(): AppResult<RecordingRecoveryResult> {
        return mutex.withLock {
            activeSegment = null
            var recoveredCount = 0
            val sessions = sessionRepository.observeSessions().first()
            for (session in sessions) {
                if (session.status != PodcastSessionStatus.RECORDING && session.activeSegmentId == null) {
                    continue
                }

                val detail = sessionRepository.observeSessionDetail(session.id).first()
                val interruptedSegment = detail?.recordingSegments
                    ?.firstOrNull { it.id == session.activeSegmentId }
                    ?: detail?.recordingSegments?.lastOrNull { it.status == RecordingSegmentStatus.RECORDING }

                if (interruptedSegment != null) {
                    val recoveredAt = nowMillis()
                    val endedAt = interruptedSegment.endedAt ?: recoveredAt
                    val updatedSegment = interruptedSegment.copy(
                        status = RecordingSegmentStatus.INTERRUPTED,
                        endedAt = endedAt,
                        durationMs = (endedAt - interruptedSegment.startedAt).coerceAtLeast(0L),
                        errorMessage = "Recording interrupted before pause completed",
                        updatedAt = recoveredAt
                    )
                    when (val segmentResult = sessionRepository.updateSegment(updatedSegment)) {
                        is AppResult.Success -> Unit
                        is AppResult.Failure -> return@withLock segmentResult
                    }
                }

                val recoveredStatus = if (
                    interruptedSegment != null ||
                    detail?.recordingSegments?.isNotEmpty() == true ||
                    session.transcript.isNotBlank()
                ) {
                    PodcastSessionStatus.PAUSED
                } else {
                    PodcastSessionStatus.ERROR
                }
                val recoveredError = if (recoveredStatus == PodcastSessionStatus.ERROR) {
                    "Recording metadata incomplete after recovery"
                } else {
                    session.errorMessage
                }
                when (val statusResult = sessionRepository.updateStatus(
                    sessionId = session.id,
                    status = recoveredStatus,
                    errorMessage = recoveredError
                )) {
                    is AppResult.Success -> recoveredCount += 1
                    is AppResult.Failure -> return@withLock statusResult
                }
            }

            AppResult.Success(RecordingRecoveryResult(recoveredSessionCount = recoveredCount))
        }
    }

    private suspend fun pauseActiveBeforeSwitchLocked(): AppResult<RecordingControllerState> {
        val active = activeSegment ?: return AppResult.Success(RecordingControllerState.Idle)
        return pauseActiveLocked(active)
    }

    private suspend fun startSegmentLocked(
        sessionId: String,
        sourceType: AudioSourceType
    ): AppResult<RecordingControllerState> {
        val startedAt = nowMillis()
        val segment = when (val result = sessionRepository.appendSegment(sessionId, sourceType, startedAt)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val request = SegmentStartRequest(
            sessionId = sessionId,
            segmentId = segment.id,
            sourceType = sourceType
        )

        when (val startResult = recorder.start(request)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> {
                markSegmentError(segment, startResult.error)
                sessionRepository.updateStatus(
                    sessionId = sessionId,
                    status = PodcastSessionStatus.ERROR,
                    errorMessage = "Recording segment failed to start"
                )
                return startResult
            }
        }

        when (val statusResult = sessionRepository.updateStatus(
            sessionId = sessionId,
            status = PodcastSessionStatus.RECORDING
        )) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return statusResult
        }

        val active = ActiveRecordingSegment(
            sessionId = sessionId,
            segmentId = segment.id,
            sourceType = sourceType
        )
        activeSegment = active
        return AppResult.Success(toState(active))
    }

    private suspend fun pauseActiveLocked(
        active: ActiveRecordingSegment
    ): AppResult<RecordingControllerState> {
        val stopResult = when (val result = recorder.stop(active)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                activeSegment = null
                sessionRepository.updateStatus(
                    sessionId = active.sessionId,
                    status = PodcastSessionStatus.ERROR,
                    errorMessage = "Recording segment failed to stop"
                )
                return result
            }
        }
        activeSegment = null

        val detail = sessionRepository.observeSessionDetail(active.sessionId).first()
        val segment = detail?.recordingSegments
            ?.firstOrNull { it.id == active.segmentId }
        if (segment != null) {
            val completed = segment.copy(
                status = RecordingSegmentStatus.COMPLETED,
                endedAt = stopResult.endedAt,
                durationMs = stopResult.durationMs,
                updatedAt = stopResult.endedAt
            )
            when (val updateResult = sessionRepository.updateSegment(completed)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return updateResult
            }
        }

        when (val statusResult = sessionRepository.updateStatus(
            sessionId = active.sessionId,
            status = PodcastSessionStatus.PAUSED
        )) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return statusResult
        }

        return AppResult.Success(RecordingControllerState.Idle)
    }

    private suspend fun markSegmentError(segment: RecordingSegment, error: AppError) {
        sessionRepository.updateSegment(
            segment.copy(
                status = RecordingSegmentStatus.ERROR,
                errorMessage = error.safeMessage(),
                updatedAt = nowMillis()
            )
        )
    }

    private fun toState(active: ActiveRecordingSegment): RecordingControllerState {
        return RecordingControllerState(
            activeSessionId = active.sessionId,
            activeSegmentId = active.segmentId,
            sourceType = active.sourceType
        )
    }
}

private fun AppError.safeMessage(): String {
    return when (this) {
        is AppError.AudioRecordInitFailed -> "Audio recorder failed to start"
        is AppError.RecordingPipelineFailed -> "Recording pipeline failed"
        is AppError.Unknown -> "Recording action failed"
        else -> this::class.simpleName ?: "Recording action failed"
    }
}
