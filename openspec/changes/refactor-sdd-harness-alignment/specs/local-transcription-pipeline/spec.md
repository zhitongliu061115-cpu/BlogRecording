## ADDED Requirements

### Requirement: PCM chunking preserves current timing behavior
The app SHALL use the configured transcription chunk duration clamped to 10 seconds through 600 seconds and emit fixed PCM chunks for recognition.

#### Scenario: Chunk duration is outside range
- **WHEN** stored settings contain a transcription chunk duration below 10 seconds or above 600 seconds
- **THEN** the pipeline uses the nearest supported boundary

### Requirement: VAD setting is preserved without changing current main-pipeline behavior
The app SHALL keep current behavior where VAD can be configured but the main recording pipeline uses fixed PCM chunks and recognition windows.

#### Scenario: VAD is enabled
- **WHEN** VAD is enabled in Settings
- **THEN** the main recording pipeline still processes fixed PCM chunks unless a later spec changes that behavior

### Requirement: Recognition windows are bounded
The app SHALL split recognition work so each recognizer segment is no longer than 30 seconds.

#### Scenario: PCM chunk exceeds recognition window
- **WHEN** a PCM chunk is longer than 30 seconds
- **THEN** recognition is performed over multiple bounded segments

### Requirement: Transcript segments include timing and speaker data
The app SHALL assemble recognized text with session id, timestamps, language/confidence data when available, and speaker labels.

#### Scenario: Recognizer returns text
- **WHEN** recognition returns non-blank text
- **THEN** a transcript segment is appended and the session transcript is rebuilt
