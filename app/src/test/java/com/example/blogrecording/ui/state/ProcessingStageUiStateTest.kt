package com.example.blogrecording.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingStageUiStateTest {
    @Test
    fun internalAudioUnavailableExplainsSystemCaptureFallbacks() {
        val state = ProcessingStageUiState.internalAudioUnavailable()

        assertEquals(ProcessingStage.SILENCE, state.stage)
        assertTrue(state.isWarning)
        assertTrue(state.message.contains("源 App"))
        assertTrue(state.message.contains("可捕获音源"))
        assertTrue(state.message.contains("扬声器"))
        assertTrue(state.message.contains("麦克风"))
    }

    @Test
    fun authorizingSystemAudioMentionsScreenAudioCaptureWithoutPrivilegedPermission() {
        val state = ProcessingStageUiState.authorizingSystemAudio()

        assertEquals(ProcessingStage.AUTHORIZING, state.stage)
        assertTrue(state.isActive)
        assertTrue(state.message.contains("屏幕"))
        assertTrue(state.message.contains("音频捕获"))
        assertFalseSystemLevelLanguage(state.title)
        assertFalseSystemLevelLanguage(state.message)
    }

    @Test
    fun mediaProjectionDeniedMessagePointsToCaptureAuthorizationAndMicrophoneFallback() {
        val state = ProcessingStageUiState.mediaProjectionDenied()

        assertEquals(ProcessingStage.ERROR, state.stage)
        assertTrue(state.isWarning)
        assertTrue(state.message.contains("屏幕"))
        assertTrue(state.message.contains("音频捕获"))
        assertTrue(state.message.contains("麦克风"))
        assertFalseSystemLevelLanguage(state.message)
    }

    private fun assertFalseSystemLevelLanguage(text: String) {
        assertTrue(!text.contains("系统级"))
        assertTrue(!text.contains("系统音频输出"))
    }
}
