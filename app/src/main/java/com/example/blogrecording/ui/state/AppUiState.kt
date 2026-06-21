package com.example.blogrecording.ui.state

import com.example.blogrecording.common.AppError
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.ModelLoadStatus
import com.example.blogrecording.data.ModelStatus
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.TranscriptSegmentEntity

enum class AppScreen {
    HOME,
    SETTINGS,
    HISTORY,
    DETAIL
}

data class AppUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val home: HomeUiState = HomeUiState(),
    val settings: AppSettings = AppSettings(),
    val hasApiKey: Boolean = false,
    val currentSession: RecordingSessionEntity? = null,
    val currentPodcastSummary: SessionSummary? = null,
    val currentTagLabels: List<String> = emptyList(),
    val sessions: List<RecordingSessionEntity> = emptyList(),
    val currentSegments: List<TranscriptSegmentEntity> = emptyList(),
    val selectedSessionId: String? = null,
    val recordingStatus: RecordingStatus = RecordingStatus.NOT_STARTED,
    val audioSourceType: AudioSourceType? = null,
    val vadLabel: String = "未开始",
    val processingStage: ProcessingStageUiState = ProcessingStageUiState.idle(),
    val processingSessionId: String? = null,
    val lastSpeechAtMs: Long? = null,
    val silentDurationMs: Long = 0L,
    val modelStatus: ModelStatus = ModelStatus(
        senseVoice = ModelLoadStatus.MISSING,
        vad = ModelLoadStatus.MISSING,
        diarization = ModelLoadStatus.MISSING
    ),
    val isGeneratingSummary: Boolean = false,
    val error: AppError? = null
)
