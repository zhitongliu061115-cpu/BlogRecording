package com.example.blogrecording.data

import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(
        title: String? = null,
        sourceType: AudioSourceType? = null
    ): PodcastSession

    suspend fun renameSession(sessionId: String, title: String): AppResult<PodcastSession>

    suspend fun appendSegment(
        sessionId: String,
        sourceType: AudioSourceType,
        startedAt: Long
    ): AppResult<RecordingSegment>

    suspend fun updateSegment(segment: RecordingSegment): AppResult<RecordingSegment>

    suspend fun updateStatus(
        sessionId: String,
        status: PodcastSessionStatus,
        errorMessage: String? = null
    ): AppResult<PodcastSession>

    suspend fun updateSummaryLifecycle(
        sessionId: String,
        status: SummaryStatus,
        modelName: String,
        summaryText: String? = null,
        generatedAt: Long? = null,
        errorMessage: String? = null
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("summary lifecycle unsupported"))
    }

    fun observeSessions(): Flow<List<PodcastSession>>

    fun observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?>
}
