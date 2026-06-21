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

    suspend fun createImportedSession(
        title: String,
        metadata: ImportedContentMetadata
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("imported session unsupported"))
    }

    suspend fun updateImportedContent(
        sessionId: String,
        metadata: ImportedContentMetadata,
        status: PodcastSessionStatus? = null
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("imported content unsupported"))
    }

    suspend fun appendImportedSegment(
        sessionId: String,
        startedAt: Long,
        durationMs: Long,
        sampleRate: Int,
        channelCount: Int
    ): AppResult<RecordingSegment> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("imported segment unsupported"))
    }

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
        structuredSummary: StructuredSummary? = null,
        tagGeneration: SessionTagGeneration? = null,
        highlights: SessionHighlights? = null,
        errorMessage: String? = null
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("summary lifecycle unsupported"))
    }

    suspend fun updateTagGeneration(
        sessionId: String,
        tagGeneration: SessionTagGeneration
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("tag generation unsupported"))
    }

    suspend fun updateHighlights(
        sessionId: String,
        highlights: SessionHighlights
    ): AppResult<PodcastSession> {
        return AppResult.Failure(com.example.blogrecording.common.AppError.Unknown("highlights unsupported"))
    }

    fun observeSessions(): Flow<List<PodcastSession>>

    fun observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?>
}
