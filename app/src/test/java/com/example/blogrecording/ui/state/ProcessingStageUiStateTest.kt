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
        assertTrue(state.message.contains("麦克风"))
    }
}
