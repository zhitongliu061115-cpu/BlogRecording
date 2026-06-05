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
    val transcriptionChunkDurationMs: Long = 180_000L,
    val firstRunPrivacyAccepted: Boolean = false
)

data class ModelStatus(
    val senseVoice: ModelLoadStatus,
    val vad: ModelLoadStatus,
    val diarization: ModelLoadStatus
)
