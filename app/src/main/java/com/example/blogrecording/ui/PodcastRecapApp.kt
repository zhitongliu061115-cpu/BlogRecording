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
    onStartMic: () -> Unit = viewModel::startMicrophoneRecording
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        when (state.currentScreen) {
            AppScreen.HOME -> HomeScreen(
                state = state,
                onStartInternal = onStartInternal,
                onStartMic = onStartMic,
                onStop = viewModel::stopRecording,
                onGenerateSummary = viewModel::generateSummaryForCurrent,
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
                onDelete = viewModel::deleteCurrentSession
            )
        }
        if (!state.settings.firstRunPrivacyAccepted) {
            PrivacyNoticeDialog(onAccept = viewModel::acceptPrivacy)
        }
    }
}
