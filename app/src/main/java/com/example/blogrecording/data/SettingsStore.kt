package com.example.blogrecording.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = SettingsContract.DATASTORE_NAME)

class SettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val defaults = SettingsContract.DEFAULT_SETTINGS
        AppSettings(
            deepSeekModel = prefs[Keys.DeepSeekModel] ?: defaults.deepSeekModel,
            summaryLanguage = prefs[Keys.SummaryLanguage]?.toEnumOrDefault(SummaryLanguage.CHINESE)
                ?: defaults.summaryLanguage,
            summaryStyle = prefs[Keys.SummaryStyle]?.let { SummaryStyle.fromLegacyName(it) }
                ?: defaults.summaryStyle,
            sherpaModelRootPath = prefs[Keys.SherpaModelRootPath] ?: defaults.sherpaModelRootPath,
            senseVoiceModelPath = prefs[Keys.SenseVoiceModelPath] ?: defaults.senseVoiceModelPath,
            vadModelPath = prefs[Keys.VadModelPath] ?: defaults.vadModelPath,
            diarizationModelPath = prefs[Keys.DiarizationModelPath] ?: defaults.diarizationModelPath,
            enableVad = prefs[Keys.EnableVad] ?: defaults.enableVad,
            enableSpeakerDiarization = prefs[Keys.EnableSpeakerDiarization] ?: defaults.enableSpeakerDiarization,
            maxSpeakerCount = prefs[Keys.MaxSpeakerCount] ?: defaults.maxSpeakerCount,
            vadSpeechThreshold = prefs[Keys.VadSpeechThreshold] ?: defaults.vadSpeechThreshold,
            minSpeechDurationMs = prefs[Keys.MinSpeechDurationMs] ?: defaults.minSpeechDurationMs,
            minSilenceDurationMs = prefs[Keys.MinSilenceDurationMs] ?: defaults.minSilenceDurationMs,
            maxSpeechDurationMs = prefs[Keys.MaxSpeechDurationMs] ?: defaults.maxSpeechDurationMs,
            transcriptionChunkDurationMs = prefs[Keys.TranscriptionChunkDurationMs]
                ?: defaults.transcriptionChunkDurationMs,
            firstRunPrivacyAccepted = prefs[Keys.FirstRunPrivacyAccepted] ?: defaults.firstRunPrivacyAccepted
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        val clamped = SettingsContract.clampForSave(settings)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DeepSeekModel] = clamped.deepSeekModel
            prefs[Keys.SummaryLanguage] = clamped.summaryLanguage.name
            prefs[Keys.SummaryStyle] = clamped.summaryStyle.name
            prefs[Keys.SherpaModelRootPath] = clamped.sherpaModelRootPath
            prefs[Keys.SenseVoiceModelPath] = clamped.senseVoiceModelPath
            prefs[Keys.VadModelPath] = clamped.vadModelPath
            prefs[Keys.DiarizationModelPath] = clamped.diarizationModelPath
            prefs[Keys.EnableVad] = clamped.enableVad
            prefs[Keys.EnableSpeakerDiarization] = clamped.enableSpeakerDiarization
            prefs[Keys.MaxSpeakerCount] = clamped.maxSpeakerCount
            prefs[Keys.VadSpeechThreshold] = clamped.vadSpeechThreshold
            prefs[Keys.MinSpeechDurationMs] = clamped.minSpeechDurationMs
            prefs[Keys.MinSilenceDurationMs] = clamped.minSilenceDurationMs
            prefs[Keys.MaxSpeechDurationMs] = clamped.maxSpeechDurationMs
            prefs[Keys.TranscriptionChunkDurationMs] = clamped.transcriptionChunkDurationMs
            prefs[Keys.FirstRunPrivacyAccepted] = clamped.firstRunPrivacyAccepted
        }
    }

    suspend fun updateModelPaths(paths: BundledModelPaths) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SherpaModelRootPath] = paths.sherpaModelRootPath
            prefs[Keys.SenseVoiceModelPath] = paths.senseVoiceModelPath
            prefs[Keys.VadModelPath] = paths.vadModelPath
            prefs[Keys.DiarizationModelPath] = paths.diarizationModelPath
        }
    }

    suspend fun acceptPrivacyNotice() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FirstRunPrivacyAccepted] = true
        }
    }

    private object Keys {
        val DeepSeekModel = stringPreferencesKey(SettingsContract.Keys.DEEP_SEEK_MODEL)
        val SummaryLanguage = stringPreferencesKey(SettingsContract.Keys.SUMMARY_LANGUAGE)
        val SummaryStyle = stringPreferencesKey(SettingsContract.Keys.SUMMARY_STYLE)
        val SherpaModelRootPath = stringPreferencesKey(SettingsContract.Keys.SHERPA_MODEL_ROOT_PATH)
        val SenseVoiceModelPath = stringPreferencesKey(SettingsContract.Keys.SENSE_VOICE_MODEL_PATH)
        val VadModelPath = stringPreferencesKey(SettingsContract.Keys.VAD_MODEL_PATH)
        val DiarizationModelPath = stringPreferencesKey(SettingsContract.Keys.DIARIZATION_MODEL_PATH)
        val EnableVad = booleanPreferencesKey(SettingsContract.Keys.ENABLE_VAD)
        val EnableSpeakerDiarization = booleanPreferencesKey(SettingsContract.Keys.ENABLE_SPEAKER_DIARIZATION)
        val MaxSpeakerCount = intPreferencesKey(SettingsContract.Keys.MAX_SPEAKER_COUNT)
        val VadSpeechThreshold = floatPreferencesKey(SettingsContract.Keys.VAD_SPEECH_THRESHOLD)
        val MinSpeechDurationMs = longPreferencesKey(SettingsContract.Keys.MIN_SPEECH_DURATION_MS)
        val MinSilenceDurationMs = longPreferencesKey(SettingsContract.Keys.MIN_SILENCE_DURATION_MS)
        val MaxSpeechDurationMs = longPreferencesKey(SettingsContract.Keys.MAX_SPEECH_DURATION_MS)
        val TranscriptionChunkDurationMs = longPreferencesKey(SettingsContract.Keys.TRANSCRIPTION_CHUNK_DURATION_MS)
        val FirstRunPrivacyAccepted = booleanPreferencesKey(SettingsContract.Keys.FIRST_RUN_PRIVACY_ACCEPTED)
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { it.name == this } ?: default
}
