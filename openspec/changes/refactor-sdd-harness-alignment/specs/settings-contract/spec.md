## ADDED Requirements

### Requirement: Settings DataStore contract remains compatible
The app SHALL keep the settings DataStore name `podcast_recap_settings` and preference keys `deep_seek_model`, `summary_language`, `summary_style`, `sherpa_model_root_path`, `sense_voice_model_path`, `vad_model_path`, `diarization_model_path`, `enable_vad`, `enable_speaker_diarization`, `max_speaker_count`, `vad_speech_threshold`, `min_speech_duration_ms`, `min_silence_duration_ms`, `max_speech_duration_ms`, `transcription_chunk_duration_ms`, and `first_run_privacy_accepted`.

#### Scenario: Existing settings are read
- **WHEN** preferences exist under the current keys
- **THEN** the app reads them into `AppSettings` without migration

### Requirement: Settings defaults remain stable
The app SHALL preserve defaults: `deepseek-chat`, `CHINESE`, `POINTS_QUOTES_ACTIONS`, empty model paths, `enableVad=true`, `enableSpeakerDiarization=true`, `maxSpeakerCount=4`, `vadSpeechThreshold=0.5`, `minSpeechDurationMs=300`, `minSilenceDurationMs=500`, `maxSpeechDurationMs=30000`, `transcriptionChunkDurationMs=180000`, and `firstRunPrivacyAccepted=false`.

#### Scenario: No settings are stored
- **WHEN** the settings DataStore is empty
- **THEN** defaults match the current `AppSettings` defaults

### Requirement: Settings numeric ranges are clamped
The app SHALL clamp only `max_speaker_count` to 2 through 8, `vad_speech_threshold` to 0 through 1, and `transcription_chunk_duration_ms` to 10000 through 600000 when saving; other duration settings SHALL preserve current save behavior.

#### Scenario: User saves out-of-range values
- **WHEN** settings contain out-of-range numeric values
- **THEN** stored values are clamped to the supported boundaries

### Requirement: Privacy acceptance persists independently
The app SHALL persist first-run privacy acceptance using `first_run_privacy_accepted`.

#### Scenario: Privacy accepted
- **WHEN** the user accepts the notice
- **THEN** `first_run_privacy_accepted` is stored as true
