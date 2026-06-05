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

private val Context.settingsDataStore by preferencesDataStore(name = "podcast_recap_settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            deepSeekModel = prefs[Keys.DeepSeekModel] ?: "deepseek-chat",
            summaryLanguage = prefs[Keys.SummaryLanguage]?.toEnumOrDefault(SummaryLanguage.CHINESE)
                ?: SummaryLanguage.CHINESE,
            summaryStyle = prefs[Keys.SummaryStyle]?.toEnumOrDefault(SummaryStyle.POINTS_QUOTES_ACTIONS)
                ?: SummaryStyle.POINTS_QUOTES_ACTIONS,
            sherpaModelRootPath = prefs[Keys.SherpaModelRootPath] ?: "",
            senseVoiceModelPath = prefs[Keys.SenseVoiceModelPath] ?: "",
            vadModelPath = prefs[Keys.VadModelPath] ?: "",
            diarizationModelPath = prefs[Keys.DiarizationModelPath] ?: "",
            enableVad = prefs[Keys.EnableVad] ?: true,
            enableSpeakerDiarization = prefs[Keys.EnableSpeakerDiarization] ?: true,
            maxSpeakerCount = prefs[Keys.MaxSpeakerCount] ?: 4,
            vadSpeechThreshold = prefs[Keys.VadSpeechThreshold] ?: 0.5f,
            minSpeechDurationMs = prefs[Keys.MinSpeechDurationMs] ?: 300L,
            minSilenceDurationMs = prefs[Keys.MinSilenceDurationMs] ?: 500L,
            maxSpeechDurationMs = prefs[Keys.MaxSpeechDurationMs] ?: 30_000L,
            transcriptionChunkDurationMs = prefs[Keys.TranscriptionChunkDurationMs] ?: 180_000L,
            firstRunPrivacyAccepted = prefs[Keys.FirstRunPrivacyAccepted] ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DeepSeekModel] = settings.deepSeekModel
            prefs[Keys.SummaryLanguage] = settings.summaryLanguage.name
            prefs[Keys.SummaryStyle] = settings.summaryStyle.name
            prefs[Keys.SherpaModelRootPath] = settings.sherpaModelRootPath
            prefs[Keys.SenseVoiceModelPath] = settings.senseVoiceModelPath
            prefs[Keys.VadModelPath] = settings.vadModelPath
            prefs[Keys.DiarizationModelPath] = settings.diarizationModelPath
            prefs[Keys.EnableVad] = settings.enableVad
            prefs[Keys.EnableSpeakerDiarization] = settings.enableSpeakerDiarization
            prefs[Keys.MaxSpeakerCount] = settings.maxSpeakerCount.coerceIn(2, 8)
            prefs[Keys.VadSpeechThreshold] = settings.vadSpeechThreshold.coerceIn(0f, 1f)
            prefs[Keys.MinSpeechDurationMs] = settings.minSpeechDurationMs
            prefs[Keys.MinSilenceDurationMs] = settings.minSilenceDurationMs
            prefs[Keys.MaxSpeechDurationMs] = settings.maxSpeechDurationMs
            prefs[Keys.TranscriptionChunkDurationMs] = settings.transcriptionChunkDurationMs.coerceIn(10_000L, 600_000L)
            prefs[Keys.FirstRunPrivacyAccepted] = settings.firstRunPrivacyAccepted
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
        val DeepSeekModel = stringPreferencesKey("deep_seek_model")
        val SummaryLanguage = stringPreferencesKey("summary_language")
        val SummaryStyle = stringPreferencesKey("summary_style")
        val SherpaModelRootPath = stringPreferencesKey("sherpa_model_root_path")
        val SenseVoiceModelPath = stringPreferencesKey("sense_voice_model_path")
        val VadModelPath = stringPreferencesKey("vad_model_path")
        val DiarizationModelPath = stringPreferencesKey("diarization_model_path")
        val EnableVad = booleanPreferencesKey("enable_vad")
        val EnableSpeakerDiarization = booleanPreferencesKey("enable_speaker_diarization")
        val MaxSpeakerCount = intPreferencesKey("max_speaker_count")
        val VadSpeechThreshold = floatPreferencesKey("vad_speech_threshold")
        val MinSpeechDurationMs = longPreferencesKey("min_speech_duration_ms")
        val MinSilenceDurationMs = longPreferencesKey("min_silence_duration_ms")
        val MaxSpeechDurationMs = longPreferencesKey("max_speech_duration_ms")
        val TranscriptionChunkDurationMs = longPreferencesKey("transcription_chunk_duration_ms")
        val FirstRunPrivacyAccepted = booleanPreferencesKey("first_run_privacy_accepted")
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { it.name == this } ?: default
}
