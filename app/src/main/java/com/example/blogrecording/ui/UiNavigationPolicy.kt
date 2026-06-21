package com.example.blogrecording.ui

import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState

object UiNavigationPolicy {
    fun navigate(state: AppUiState, screen: AppScreen): AppUiState {
        return state.copy(currentScreen = screen, error = null)
    }

    fun openDetail(
        state: AppUiState,
        sessionId: String,
        session: RecordingSessionEntity?,
        segments: List<TranscriptSegmentEntity>
    ): AppUiState {
        return state.copy(
            currentScreen = AppScreen.DETAIL,
            selectedSessionId = sessionId,
            currentSession = session,
            currentSegments = segments,
            error = null
        )
    }

    fun deleteCurrentSession(state: AppUiState): AppUiState {
        return state.copy(
            currentScreen = AppScreen.HISTORY,
            currentSession = null,
            currentPodcastSummary = null,
            currentSegments = emptyList(),
            selectedSessionId = null
        )
    }
}
