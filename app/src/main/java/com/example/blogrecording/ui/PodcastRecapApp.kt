package com.example.blogrecording.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.blogrecording.export.SessionExportFormat
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
    onSaveExport: (SessionExportFormat) -> Unit = {},
    onShareExport: (SessionExportFormat) -> Unit = {},
    onAskQuestion: (String) -> Unit = viewModel::askQuestionForCurrentSession,
    onRetryQuestion: (String) -> Unit = viewModel::retryQaForCurrentSession,
    onResumeInternalSession: (String) -> Unit = onStartInternalSession,
    onResumeMicrophoneSession: (String) -> Unit = onStartMicrophoneSession
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            bottomBar = {
                if (UiNavigationPolicy.isTopLevel(state.currentScreen)) {
                    AppBottomNavigation(
                        currentScreen = state.currentScreen,
                        onNavigate = viewModel::navigate
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(innerPadding)
            ) {
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
                    AppScreen.AI -> AiChatScreen(
                        state = state.aiChat,
                        onSelectPodcast = viewModel::selectAiPodcast,
                        onNewConversation = viewModel::startNewAiConversation,
                        onDraftChange = viewModel::updateAiDraft,
                        onSend = viewModel::sendAiDraft
                    )
                    AppScreen.MINE -> MineScreen(
                        state = state,
                        onNavigate = viewModel::navigate
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        state = state,
                        onBack = { viewModel.navigate(AppScreen.MINE) },
                        onSaveSettings = viewModel::updateSettings,
                        onSaveApiKey = viewModel::saveApiKey,
                        onDeleteApiKey = viewModel::deleteApiKey
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        state = state,
                        onOpenDetail = viewModel::openDetail,
                        onBack = { viewModel.navigate(AppScreen.MINE) }
                    )
                    AppScreen.DETAIL -> DetailScreen(
                        state = state,
                        onBack = { viewModel.navigate(AppScreen.HISTORY) },
                        onGenerateSummary = viewModel::generateSummaryForCurrent,
                        onToggleHighlightFavorite = viewModel::toggleHighlightFavorite,
                        onSaveExport = onSaveExport,
                        onShareExport = onShareExport,
                        onAskQuestion = onAskQuestion,
                        onRetryQuestion = onRetryQuestion,
                        onDelete = viewModel::deleteCurrentSession
                    )
                }
            }
        }
        if (!state.settings.firstRunPrivacyAccepted) {
            PrivacyNoticeDialog(onAccept = viewModel::acceptPrivacy)
        }
    }
}

@Composable
private fun AppBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(tonalElevation = 3.dp) {
        UiNavigationPolicy.topLevelScreens.forEach { screen ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onNavigate(screen) },
                icon = { Text(screen.bottomLabel()) },
                label = { Text(screen.bottomLabel()) }
            )
        }
    }
}

private fun AppScreen.bottomLabel(): String {
    return when (this) {
        AppScreen.HOME -> "首页"
        AppScreen.AI -> "AI"
        AppScreen.MINE -> "我的"
        AppScreen.SETTINGS,
        AppScreen.HISTORY,
        AppScreen.DETAIL -> ""
    }
}
