package com.example.blogrecording.data

internal object RecordingPersistenceContract {
    const val DATASTORE_NAME = "podcast_recap_records"
    const val SESSION_ORDER_KEY = "session_order"

    // Existing key families are compatibility surfaces for old recording history.
    fun sessionKey(id: String) = "session_$id"
    fun segmentsKey(sessionId: String) = "segments_$sessionId"
    fun speakersKey(sessionId: String) = "speakers_$sessionId"

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
}
