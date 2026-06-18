## ADDED Requirements

### Requirement: Recording sessions persist lifecycle states
The app SHALL preserve all current recording status enum names and persist sessions as they move through `NOT_STARTED`, `CAPTURING_AUDIO`, `VAD_DETECTING`, `DIARIZING`, `TRANSCRIBING`, `SUMMARIZING`, `COMPLETED`, and `ERROR`.

#### Scenario: Recording starts successfully
- **WHEN** capture starts after required gates pass
- **THEN** the current session is saved with `CAPTURING_AUDIO`

#### Scenario: Recording fails
- **WHEN** the recording pipeline reports a non-recoverable error
- **THEN** the current session is saved with `ERROR` and an error message

### Requirement: Stop recording flushes pending audio
The app SHALL stop active capture, flush the final PCM chunk, finish queued transcription work, and save a completed session when no error occurred.

#### Scenario: Stop active recording
- **WHEN** the user stops an active recording
- **THEN** the app transitions through transcribing stop state and saves the session as `COMPLETED`

### Requirement: Internal silence is recoverable
The app SHALL treat internal-audio silence as a recoverable waiting state instead of failing the session.

#### Scenario: Internal audio is silent
- **WHEN** the internal capture manager reports `InternalAudioSilent`
- **THEN** the app keeps capturing and shows a waiting-for-system-audio status

### Requirement: Model gates run before capture
The app SHALL block recording when required SenseVoice, VAD, or diarization models are missing for the enabled settings.

#### Scenario: SenseVoice model is missing
- **WHEN** recording starts and SenseVoice status is `MISSING`
- **THEN** the session is saved as `ERROR` with `SenseVoiceModelMissing`
