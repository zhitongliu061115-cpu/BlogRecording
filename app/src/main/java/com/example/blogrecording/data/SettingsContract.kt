package com.example.blogrecording.data

internal object SettingsContract {
    const val DATASTORE_NAME = "podcast_recap_settings"

    object Keys {
        const val DEEP_SEEK_MODEL = "deep_seek_model"
        const val SUMMARY_LANGUAGE = "summary_language"
        const val SUMMARY_STYLE = "summary_style"
        const val SHERPA_MODEL_ROOT_PATH = "sherpa_model_root_path"
        const val SENSE_VOICE_MODEL_PATH = "sense_voice_model_path"
        const val VAD_MODEL_PATH = "vad_model_path"
        const val DIARIZATION_MODEL_PATH = "diarization_model_path"
        const val ENABLE_VAD = "enable_vad"
        const val ENABLE_SPEAKER_DIARIZATION = "enable_speaker_diarization"
        const val MAX_SPEAKER_COUNT = "max_speaker_count"
        const val VAD_SPEECH_THRESHOLD = "vad_speech_threshold"
        const val MIN_SPEECH_DURATION_MS = "min_speech_duration_ms"
        const val MIN_SILENCE_DURATION_MS = "min_silence_duration_ms"
        const val MAX_SPEECH_DURATION_MS = "max_speech_duration_ms"
        const val TRANSCRIPTION_CHUNK_DURATION_MS = "transcription_chunk_duration_ms"
        const val FIRST_RUN_PRIVACY_ACCEPTED = "first_run_privacy_accepted"
    }

    val DEFAULT_SETTINGS = AppSettings()

    fun clampForSave(settings: AppSettings): AppSettings {
        return settings.copy(
            maxSpeakerCount = settings.maxSpeakerCount.coerceIn(2, 8),
            vadSpeechThreshold = settings.vadSpeechThreshold.coerceIn(0f, 1f),
            transcriptionChunkDurationMs = settings.transcriptionChunkDurationMs.coerceIn(10_000L, 600_000L)
        )
    }
}
