package com.example.blogrecording.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingStatusRecoveryTest {
    @Test
    fun onlyActiveStatusesAreMarkedInterruptedOnStartup() {
        val activeStatuses = listOf(
            RecordingStatus.CAPTURING_AUDIO,
            RecordingStatus.VAD_DETECTING,
            RecordingStatus.DIARIZING,
            RecordingStatus.TRANSCRIBING,
            RecordingStatus.SUMMARIZING
        )
        val inactiveStatuses = listOf(
            RecordingStatus.NOT_STARTED,
            RecordingStatus.COMPLETED,
            RecordingStatus.ERROR
        )

        assertTrue(activeStatuses.all { it.isInterruptedOnStartup() })
        assertFalse(inactiveStatuses.any { it.isInterruptedOnStartup() })
    }
}
