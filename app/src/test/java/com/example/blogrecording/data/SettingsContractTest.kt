package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsContractTest {
    @Test
    fun contractKeepsDatastoreNameAndPreferenceKeys() {
        assertEquals("podcast_recap_settings", SettingsContract.DATASTORE_NAME)

        val keys = setOf(
            SettingsContract.Keys.DEEP_SEEK_MODEL,
            SettingsContract.Keys.SUMMARY_LANGUAGE,
            SettingsContract.Keys.SUMMARY_STYLE,
            SettingsContract.Keys.SHERPA_MODEL_ROOT_PATH,
            SettingsContract.Keys.SENSE_VOICE_MODEL_PATH,
            SettingsContract.Keys.VAD_MODEL_PATH,
            SettingsContract.Keys.DIARIZATION_MODEL_PATH,
            SettingsContract.Keys.ENABLE_VAD,
            SettingsContract.Keys.ENABLE_SPEAKER_DIARIZATION,
            SettingsContract.Keys.MAX_SPEAKER_COUNT,
            SettingsContract.Keys.VAD_SPEECH_THRESHOLD,
            SettingsContract.Keys.MIN_SPEECH_DURATION_MS,
            SettingsContract.Keys.MIN_SILENCE_DURATION_MS,
            SettingsContract.Keys.MAX_SPEECH_DURATION_MS,
            SettingsContract.Keys.TRANSCRIPTION_CHUNK_DURATION_MS,
            SettingsContract.Keys.FIRST_RUN_PRIVACY_ACCEPTED
        )

        assertEquals(16, keys.size)
        assertTrue("transcription_chunk_duration_ms" in keys)
        assertTrue("first_run_privacy_accepted" in keys)
    }

    @Test
    fun defaultsMatchCurrentSettingsContract() {
        val defaults = SettingsContract.DEFAULT_SETTINGS

        assertEquals("deepseek-chat", defaults.deepSeekModel)
        assertEquals(SummaryLanguage.CHINESE, defaults.summaryLanguage)
        assertEquals(SummaryStyle.POINTS_QUOTES_ACTIONS, defaults.summaryStyle)
        assertTrue(defaults.enableVad)
        assertTrue(defaults.enableSpeakerDiarization)
        assertEquals(4, defaults.maxSpeakerCount)
        assertEquals(0.5f, defaults.vadSpeechThreshold)
        assertEquals(300L, defaults.minSpeechDurationMs)
        assertEquals(500L, defaults.minSilenceDurationMs)
        assertEquals(30_000L, defaults.maxSpeechDurationMs)
        assertEquals(180_000L, defaults.transcriptionChunkDurationMs)
        assertFalse(defaults.firstRunPrivacyAccepted)
    }

    @Test
    fun clampForSaveOnlyClampsDocumentedFields() {
        val settings = AppSettings(
            maxSpeakerCount = 99,
            vadSpeechThreshold = -2f,
            minSpeechDurationMs = -10L,
            minSilenceDurationMs = -20L,
            maxSpeechDurationMs = -30L,
            transcriptionChunkDurationMs = 1L
        )

        val clamped = SettingsContract.clampForSave(settings)

        assertEquals(8, clamped.maxSpeakerCount)
        assertEquals(0f, clamped.vadSpeechThreshold)
        assertEquals(10_000L, clamped.transcriptionChunkDurationMs)
        assertEquals(-10L, clamped.minSpeechDurationMs)
        assertEquals(-20L, clamped.minSilenceDurationMs)
        assertEquals(-30L, clamped.maxSpeechDurationMs)
    }
}
