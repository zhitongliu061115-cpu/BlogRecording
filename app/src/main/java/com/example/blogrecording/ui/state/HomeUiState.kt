package com.example.blogrecording.ui.state

data class HomeUiState(
    val cards: List<PodcastCardUiState> = emptyList(),
    val isEmpty: Boolean = true,
    val activeRecordingSessionId: String? = null,
    val renameDialog: RenameDialogUiState? = null,
    val errorMessage: String? = null
)

data class PodcastCardUiState(
    val sessionId: String,
    val title: String,
    val statusLabel: String,
    val durationLabel: String,
    val segmentCountLabel: String,
    val transcriptionLabel: String,
    val summaryLabel: String,
    val isRecording: Boolean,
    val actionState: RecordingActionState,
    val canRename: Boolean,
    val canFinish: Boolean,
    val canStartSummary: Boolean,
    val startSummaryDisabledReason: String?
)

data class RecordingActionState(
    val canStart: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val switchingFromAnotherSession: Boolean
)

data class RenameDialogUiState(
    val sessionId: String,
    val initialTitle: String
)
