package com.example.blogrecording.data

enum class AudioSourceType {
    INTERNAL_AUDIO,
    MICROPHONE
}

enum class RecordingStatus {
    NOT_STARTED,
    CAPTURING_AUDIO,
    VAD_DETECTING,
    DIARIZING,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    ERROR
}

enum class PodcastSessionStatus {
    DRAFT,
    RECORDING,
    PAUSED,
    PROCESSING,
    READY_FOR_SUMMARY,
    SUMMARIZING,
    SUMMARIZED,
    ERROR
}

enum class RecordingSegmentStatus {
    RECORDING,
    PAUSED,
    PROCESSING,
    COMPLETED,
    INTERRUPTED,
    ERROR
}

enum class SummaryStatus {
    NOT_READY,
    READY,
    SUMMARIZING,
    SUMMARIZED,
    FAILED
}

fun RecordingStatus.isInterruptedOnStartup(): Boolean {
    return when (this) {
        RecordingStatus.CAPTURING_AUDIO,
        RecordingStatus.VAD_DETECTING,
        RecordingStatus.DIARIZING,
        RecordingStatus.TRANSCRIBING,
        RecordingStatus.SUMMARIZING -> true
        RecordingStatus.NOT_STARTED,
        RecordingStatus.COMPLETED,
        RecordingStatus.ERROR -> false
    }
}

enum class SummaryLanguage {
    CHINESE,
    ENGLISH,
    FOLLOW_PODCAST
}

enum class SummaryStyle {
    BRIEF,
    DEEP_RECAP,
    TIMELINE_NOTES,
    POINTS_QUOTES_ACTIONS
}

enum class ModelLoadStatus {
    LOADED,
    MISSING,
    INIT_FAILED
}

data class RecordingSessionEntity(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceType: AudioSourceType,
    val status: RecordingStatus,
    val transcript: String,
    val summary: String?,
    val asrModelName: String,
    val vadModelName: String,
    val diarizationModelName: String,
    val summaryModelName: String,
    val summaryStyle: SummaryStyle,
    val summaryLanguage: SummaryLanguage,
    val detectedSpeakerCount: Int,
    val segmentCount: Int,
    val errorMessage: String?
)

data class TranscriptSegmentEntity(
    val id: String,
    val sessionId: String,
    val recordingSegmentId: String?,
    val startMs: Long,
    val endMs: Long,
    val speakerId: String,
    val speakerDisplayName: String,
    val text: String,
    val language: String?,
    val confidence: Float?,
    val vadConfidence: Float?,
    val isFinal: Boolean,
    val createdAt: Long
)

data class SpeakerProfileEntity(
    val id: String,
    val sessionId: String,
    val speakerId: String,
    val displayName: String,
    val colorIndex: Int,
    val segmentCount: Int,
    val totalSpeechDurationMs: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class SessionSummary(
    val text: String,
    val status: SummaryStatus,
    val modelName: String,
    val generatedAt: Long?,
    val updatedAt: Long,
    val errorMessage: String?
)

data class RecordingSegment(
    val id: String,
    val sessionId: String,
    val index: Int,
    val sourceType: AudioSourceType,
    val status: RecordingSegmentStatus,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long,
    val pcmFilePath: String?,
    val audioFilePath: String?,
    val sampleRate: Int?,
    val channelCount: Int?,
    val transcriptSegmentIds: List<String>,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class PodcastSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceType: AudioSourceType?,
    val status: PodcastSessionStatus,
    val activeSegmentId: String?,
    val lastCompletedSegmentId: String?,
    val transcript: String,
    val summary: SessionSummary?,
    val summaryStyle: SummaryStyle,
    val summaryLanguage: SummaryLanguage,
    val summaryModelName: String,
    val asrModelName: String,
    val vadModelName: String,
    val diarizationModelName: String,
    val detectedSpeakerCount: Int,
    val recordingSegmentCount: Int,
    val transcriptSegmentCount: Int,
    val errorMessage: String?,
    val legacyRecordingSessionId: String?
)

data class PodcastSessionDetail(
    val session: PodcastSession,
    val recordingSegments: List<RecordingSegment>,
    val transcriptSegments: List<TranscriptSegmentEntity>,
    val speakerProfiles: List<SpeakerProfileEntity>
)

object PodcastSessionStateMachine {
    fun create(session: PodcastSession): PodcastSession {
        return session.copy(
            status = PodcastSessionStatus.DRAFT,
            activeSegmentId = null,
            errorMessage = null
        )
    }

    fun start(session: PodcastSession, segmentId: String): PodcastSession {
        require(session.status in STARTABLE_STATUSES) {
            "Cannot start recording from ${session.status}"
        }
        require(segmentId.isNotBlank()) { "segmentId must not be blank" }
        return session.copy(
            status = PodcastSessionStatus.RECORDING,
            activeSegmentId = segmentId,
            errorMessage = null
        )
    }

    fun pause(session: PodcastSession, completedSegmentId: String? = session.activeSegmentId): PodcastSession {
        require(session.status == PodcastSessionStatus.RECORDING) {
            "Cannot pause recording from ${session.status}"
        }
        return session.copy(
            status = PodcastSessionStatus.PAUSED,
            activeSegmentId = null,
            lastCompletedSegmentId = completedSegmentId ?: session.lastCompletedSegmentId,
            errorMessage = null
        )
    }

    fun resume(session: PodcastSession, segmentId: String): PodcastSession {
        return start(session, segmentId)
    }

    fun finalize(session: PodcastSession): PodcastSession {
        require(session.status in FINALIZABLE_STATUSES) {
            "Cannot finalize from ${session.status}"
        }
        return session.copy(
            status = PodcastSessionStatus.READY_FOR_SUMMARY,
            activeSegmentId = null,
            summary = session.summary?.copy(status = SummaryStatus.READY),
            errorMessage = null
        )
    }

    fun startTranscription(session: PodcastSession): PodcastSession {
        require(session.status in TRANSCRIPTION_STARTABLE_STATUSES) {
            "Cannot start transcription from ${session.status}"
        }
        return session.copy(status = PodcastSessionStatus.PROCESSING, errorMessage = null)
    }

    fun markReadyForSummary(session: PodcastSession): PodcastSession {
        require(session.status in READY_MARKABLE_STATUSES) {
            "Cannot mark ready for summary from ${session.status}"
        }
        require(session.transcript.isNotBlank()) { "transcript must not be blank" }
        return session.copy(
            status = PodcastSessionStatus.READY_FOR_SUMMARY,
            activeSegmentId = null,
            summary = session.summary?.copy(status = SummaryStatus.READY),
            errorMessage = null
        )
    }

    fun summarize(session: PodcastSession): PodcastSession {
        require(session.status == PodcastSessionStatus.READY_FOR_SUMMARY) {
            "Cannot summarize from ${session.status}"
        }
        return session.copy(
            status = PodcastSessionStatus.SUMMARIZING,
            summary = session.summary?.copy(status = SummaryStatus.SUMMARIZING),
            errorMessage = null
        )
    }

    fun markSummarized(session: PodcastSession, summary: SessionSummary): PodcastSession {
        require(session.status == PodcastSessionStatus.SUMMARIZING) {
            "Cannot mark summarized from ${session.status}"
        }
        return session.copy(
            status = PodcastSessionStatus.SUMMARIZED,
            summary = summary.copy(status = SummaryStatus.SUMMARIZED),
            errorMessage = null
        )
    }

    fun fail(session: PodcastSession, errorMessage: String): PodcastSession {
        return session.copy(
            status = PodcastSessionStatus.ERROR,
            activeSegmentId = null,
            errorMessage = errorMessage.takeIf { it.isNotBlank() }
        )
    }

    fun recover(session: PodcastSession): PodcastSession {
        return when (session.status) {
            PodcastSessionStatus.RECORDING,
            PodcastSessionStatus.PROCESSING,
            PodcastSessionStatus.SUMMARIZING -> {
                val recoveredStatus = if (
                    session.recordingSegmentCount > 0 ||
                    session.transcript.isNotBlank()
                ) {
                    PodcastSessionStatus.PAUSED
                } else {
                    PodcastSessionStatus.ERROR
                }
                session.copy(
                    status = recoveredStatus,
                    activeSegmentId = null,
                    errorMessage = if (recoveredStatus == PodcastSessionStatus.ERROR) {
                        session.errorMessage ?: "Interrupted session recovered without completed data"
                    } else {
                        session.errorMessage
                    }
                )
            }
            PodcastSessionStatus.DRAFT,
            PodcastSessionStatus.PAUSED,
            PodcastSessionStatus.READY_FOR_SUMMARY,
            PodcastSessionStatus.SUMMARIZED,
            PodcastSessionStatus.ERROR -> session
        }
    }

    private val STARTABLE_STATUSES = setOf(
        PodcastSessionStatus.DRAFT,
        PodcastSessionStatus.PAUSED,
        PodcastSessionStatus.READY_FOR_SUMMARY,
        PodcastSessionStatus.ERROR
    )
    private val FINALIZABLE_STATUSES = setOf(
        PodcastSessionStatus.PAUSED,
        PodcastSessionStatus.PROCESSING
    )
    private val TRANSCRIPTION_STARTABLE_STATUSES = setOf(
        PodcastSessionStatus.RECORDING,
        PodcastSessionStatus.PAUSED
    )
    private val READY_MARKABLE_STATUSES = setOf(
        PodcastSessionStatus.PROCESSING,
        PodcastSessionStatus.PAUSED
    )
}

data class AppSettings(
    val deepSeekModel: String = "deepseek-chat",
    val summaryLanguage: SummaryLanguage = SummaryLanguage.CHINESE,
    val summaryStyle: SummaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
    val sherpaModelRootPath: String = "",
    val senseVoiceModelPath: String = "",
    val vadModelPath: String = "",
    val diarizationModelPath: String = "",
    val enableVad: Boolean = true,
    val enableSpeakerDiarization: Boolean = true,
    val maxSpeakerCount: Int = 4,
    val vadSpeechThreshold: Float = 0.5f,
    val minSpeechDurationMs: Long = 300L,
    val minSilenceDurationMs: Long = 500L,
    val maxSpeechDurationMs: Long = 30_000L,
    val transcriptionChunkDurationMs: Long = 30_000L,
    val firstRunPrivacyAccepted: Boolean = false
)

data class ModelStatus(
    val senseVoice: ModelLoadStatus,
    val vad: ModelLoadStatus,
    val diarization: ModelLoadStatus
)
