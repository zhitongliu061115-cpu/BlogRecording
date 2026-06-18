## ADDED Requirements

### Requirement: Settings DataStore contract remains compatible
The app SHALL keep the settings DataStore name `podcast_recap_settings` and existing preference keys.

#### Scenario: Existing settings are read
- **WHEN** preferences exist under the current keys
- **THEN** the app reads them into `AppSettings` without migration

### Requirement: Settings defaults remain stable
The app SHALL preserve current defaults for DeepSeek model, summary language/style, model paths, VAD, speaker diarization, speaker count, VAD thresholds, speech/silence durations, chunk duration, and privacy acceptance.

#### Scenario: No settings are stored
- **WHEN** the settings DataStore is empty
- **THEN** defaults match the current `AppSettings` defaults

### Requirement: Settings numeric ranges are clamped
The app SHALL clamp `max_speaker_count` to 2 through 8, `vad_speech_threshold` to 0 through 1, and `transcription_chunk_duration_ms` to 10000 through 600000 when saving.

#### Scenario: User saves out-of-range values
- **WHEN** settings contain out-of-range numeric values
- **THEN** stored values are clamped to the supported boundaries

### Requirement: Privacy acceptance persists independently
The app SHALL persist first-run privacy acceptance using `first_run_privacy_accepted`.

#### Scenario: Privacy accepted
- **WHEN** the user accepts the notice
- **THEN** `first_run_privacy_accepted` is stored as true
