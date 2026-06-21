## ADDED Requirements

### Requirement: Official Android internal recording
The app SHALL use Android MediaProjection playback capture for internal-audio recording and SHALL NOT require privileged system audio-output permissions.

#### Scenario: Start internal recording with authorization
- **WHEN** the user starts or resumes internal recording and grants runtime permissions plus MediaProjection authorization
- **THEN** the app starts an internal-audio recording segment through the existing playback-capture path

#### Scenario: No privileged capture permission requested
- **WHEN** the app declares or checks platform permissions for the MVP
- **THEN** it does not require `CAPTURE_AUDIO_OUTPUT`, `REMOTE_SUBMIX`, root, Shizuku, or a system-signed build

### Requirement: Explain MediaProjection denial
The app SHALL present MediaProjection denial as a normal screen/audio capture authorization failure, not as a missing privileged system permission.

#### Scenario: User cancels MediaProjection
- **WHEN** the user denies or cancels the MediaProjection prompt
- **THEN** no silent internal recording starts and the user sees guidance to allow screen/audio capture or use microphone recording

### Requirement: Recover from uncapturable or silent internal audio
The app SHALL treat repeated zero internal-audio PCM as a recoverable MVP state with microphone fallback guidance.

#### Scenario: Source app emits silent capture buffers
- **WHEN** internal capture repeatedly returns zero PCM or a chunk has no meaningful audio energy
- **THEN** the app skips hallucinated transcript persistence and explains that the source app, audio route, or silence may prevent capture

#### Scenario: User chooses microphone fallback
- **WHEN** the user uses the microphone recording button after internal audio is unavailable
- **THEN** the app records through the microphone pipeline and can continue into local transcription and summary generation

### Requirement: MVP verification path
The app SHALL document a focused manual and automated verification path for internal recording, transcription, and summary.

#### Scenario: Harness is recorded
- **WHEN** the change is completed
- **THEN** the change notes list OpenSpec validation, targeted unit tests, debug build status, and the real-device MVP checklist or blocker
