package com.example.blogrecording.data

internal object RecordingPersistenceContract {
    const val DATASTORE_NAME = "podcast_recap_records"
    const val SCHEMA_VERSION_KEY = "schema_version"
    const val CURRENT_SCHEMA_VERSION = 5
    const val SESSION_ORDER_KEY = "session_order"

    // Existing key families are compatibility surfaces for old recording history.
    fun sessionKey(id: String) = "session_$id"
    fun segmentsKey(sessionId: String) = "segments_$sessionId"
    fun speakersKey(sessionId: String) = "speakers_$sessionId"
    fun recordingSegmentsKey(sessionId: String) = "recording_segments_$sessionId"

    // JSON fields persisted for the legacy single-recording session entity.
    val SESSION_FIELDS = setOf(
        "id",
        "title",
        "createdAt",
        "updatedAt",
        "sourceType",
        "status",
        "transcript",
        "summary",
        "asrModelName",
        "vadModelName",
        "diarizationModelName",
        "summaryModelName",
        "summaryStyle",
        "summaryLanguage",
        "detectedSpeakerCount",
        "segmentCount",
        "errorMessage"
    )

    // JSON fields persisted for transcript segments produced by local ASR.
    val SEGMENT_FIELDS = setOf(
        "id",
        "sessionId",
        "recordingSegmentId",
        "startMs",
        "endMs",
        "speakerId",
        "speakerDisplayName",
        "text",
        "language",
        "confidence",
        "vadConfidence",
        "isFinal",
        "createdAt"
    )

    // JSON fields persisted for speaker summaries rebuilt from transcript segments.
    val SPEAKER_PROFILE_FIELDS = setOf(
        "id",
        "sessionId",
        "speakerId",
        "displayName",
        "colorIndex",
        "segmentCount",
        "totalSpeechDurationMs",
        "createdAt",
        "updatedAt"
    )

    val PODCAST_SESSION_FIELDS = setOf(
        "id",
        "title",
        "createdAt",
        "updatedAt",
        "sourceType",
        "status",
        "activeSegmentId",
        "lastCompletedSegmentId",
        "transcript",
        "summary",
        "summaryStyle",
        "summaryLanguage",
        "summaryModelName",
        "asrModelName",
        "vadModelName",
        "diarizationModelName",
        "detectedSpeakerCount",
        "recordingSegmentCount",
        "transcriptSegmentCount",
        "errorMessage",
        "legacyRecordingSessionId",
        "importedContent"
    )

    val IMPORTED_CONTENT_FIELDS = setOf(
        "kind",
        "displayName",
        "mimeType",
        "sizeBytes",
        "durationMs",
        "status",
        "errorMessage",
        "importedAt",
        "updatedAt",
        "sourceUrl",
        "sourceHost"
    )

    val SESSION_SUMMARY_FIELDS = setOf(
        "text",
        "status",
        "modelName",
        "generatedAt",
        "updatedAt",
        "errorMessage",
        "structured"
    )

    val STRUCTURED_SUMMARY_FIELDS = setOf(
        "overview",
        "keyPoints",
        "actionItems",
        "openQuestions",
        "quoteCandidates",
        "timelineChapters",
        "parseStatus"
    )

    val TIMELINE_CHAPTER_FIELDS = setOf(
        "title",
        "startMs",
        "endMs",
        "keyPoints",
        "sourceStartMs",
        "sourceEndMs"
    )

    val RECORDING_SEGMENT_FIELDS = setOf(
        "id",
        "sessionId",
        "index",
        "sourceType",
        "status",
        "startedAt",
        "endedAt",
        "durationMs",
        "pcmFilePath",
        "audioFilePath",
        "sampleRate",
        "channelCount",
        "transcriptSegmentIds",
        "errorMessage",
        "createdAt",
        "updatedAt"
    )
}
