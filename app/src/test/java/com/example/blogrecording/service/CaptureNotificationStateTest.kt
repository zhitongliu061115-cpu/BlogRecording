package com.example.blogrecording.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureNotificationStateTest {
    @Test
    fun notificationTextIncludesPodcastTitleSourceAndState() {
        val state = CaptureNotificationState(
            podcastTitle = "Episode A",
            captureSource = CaptureNotificationState.SOURCE_MICROPHONE,
            recordingState = CaptureNotificationState.STATE_RECORDING,
            activeSessionId = "session-1"
        )

        assertEquals("Episode A", state.titleText())
        assertTrue(state.bodyText().contains("microphone"))
        assertTrue(state.bodyText().contains("recording"))
    }

    @Test
    fun notificationTextDoesNotExposeTranscriptOrFullAudioPath() {
        val state = CaptureNotificationState(
            podcastTitle = "Episode A",
            captureSource = CaptureNotificationState.SOURCE_SYSTEM_AUDIO,
            recordingState = CaptureNotificationState.STATE_PAUSED,
            activeSessionId = "session-1"
        )

        assertFalse(state.bodyText().contains("C:\\Users\\"))
        assertFalse(state.bodyText().contains("raw transcript"))
    }
}
