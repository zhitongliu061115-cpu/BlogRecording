package com.example.blogrecording.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.blogrecording.ui.components.PrivacyNoticeDialog
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState

@Composable
fun PodcastRecapApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onStartInternal: () -> Unit = {},
    onStartMicrophone: () -> Unit = {},
    onStartInternalSession: (String) -> Unit = {},
    onStartMicrophoneSession: (String) -> Unit = {},
    onImportLocalMedia: () -> Unit = {},
    onImportUrlMedia: (String) -> Unit = {},
    onResumeInternalSession: (String) -> Unit = onStartInternalSession,
    onResumeMicrophoneSession: (String) -> Unit = onStartMicrophoneSession
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        when (state.currentScreen) {
            AppScreen.HOME -> HomeScreen(
                state = state,
                onCreateSession = viewModel::createPodcastSession,
                onImportLocalMedia = onImportLocalMedia,
                onImportUrlMedia = onImportUrlMedia,
                onStartInternal = onStartInternal,
                onStartMicrophone = onStartMicrophone,
                onStartInternalSession = onStartInternalSession,
                onStartMicrophoneSession = onStartMicrophoneSession,
                onPauseRecording = viewModel::pausePodcastRecording,
                onResumeInternalSession = onResumeInternalSession,
                onResumeMicrophoneSession = onResumeMicrophoneSession,
                onFinishSession = viewModel::finishPodcastSession,
                onRequestRename = viewModel::requestRenamePodcastSession,
                onRenameSession = viewModel::renamePodcastSession,
                onDismissRename = viewModel::dismissRenamePodcastSession,
                onStartSummary = viewModel::startSummaryForPodcastSession,
                onOpenDetail = viewModel::openDetail,
                onNavigate = viewModel::navigate
            )
            AppScreen.SETTINGS -> SettingsScreen(
                state = state,
                onBack = { viewModel.navigate(AppScreen.HOME) },
                onSaveSettings = viewModel::updateSettings,
                onSaveApiKey = viewModel::saveApiKey,
                onDeleteApiKey = viewModel::deleteApiKey
            )
            AppScreen.HISTORY -> HistoryScreen(
                state = state,
                onOpenDetail = viewModel::openDetail,
                onBack = { viewModel.navigate(AppScreen.HOME) }
            )
            AppScreen.DETAIL -> DetailScreen(
                state = state,
                onBack = { viewModel.navigate(AppScreen.HISTORY) },
                onGenerateSummary = viewModel::generateSummaryForCurrent,
                onToggleHighlightFavorite = viewModel::toggleHighlightFavorite,
                onDelete = viewModel::deleteCurrentSession
            )
        }
        if (!state.settings.firstRunPrivacyAccepted) {
            PrivacyNoticeDialog(onAccept = viewModel::acceptPrivacy)
        }
    }
}
