## ADDED Requirements

### Requirement: Visible Recording Processing Stages
The app SHALL expose a user-visible processing stage for active and recently updated podcast sessions.

#### Scenario: Internal recording starts
- **WHEN** a user starts or resumes a podcast with system audio capture
- **THEN** the home card and detail screen show that system audio authorization or capture is in progress

#### Scenario: Audio is buffering
- **WHEN** recording is active and audio has been captured but not yet sent to ASR
- **THEN** the UI shows the buffered duration and the target chunk duration

#### Scenario: Chunk is transcribing
- **WHEN** a chunk is being processed by SenseVoice
- **THEN** the UI shows which chunk or segment is being transcribed

### Requirement: Silence Does Not Persist Hallucinated Transcripts
The app SHALL filter silent or near-silent audio before SenseVoice recognition, but SHALL NOT use VAD as a hard gate that prevents non-silent audio from reaching ASR.

#### Scenario: Internal audio is silent
- **WHEN** internal audio capture repeatedly emits silent buffers or a chunk has no meaningful PCM energy
- **THEN** no transcript segment is saved for that silent chunk
- **AND** the UI explains that audio is silent or the current app may not allow system capture

#### Scenario: Internal audio is non-silent
- **WHEN** internal audio capture emits a chunk with meaningful PCM energy
- **THEN** the app sends the chunk to SenseVoice even if VAD would classify it as non-speech
- **AND** the UI shows that ASR is being attempted

### Requirement: Summary Processing Feedback
The app SHALL show summary readiness and summary generation progress in the same user-facing status system.

#### Scenario: Summary cannot start
- **WHEN** summary is requested without transcript text or without a configured API key
- **THEN** the user sees the blocking reason on the card or detail screen

#### Scenario: Summary is running
- **WHEN** summary generation is in progress
- **THEN** the card and detail screen show that summarization is running until success or failure

### Requirement: Foreground Notification Stage Text
The foreground recording notification SHALL show the current high-level processing stage in Chinese.

#### Scenario: Recording stage changes
- **WHEN** the app moves between capture, buffering, transcribing, paused, or summarizing stages
- **THEN** the notification body updates to describe the new stage without exposing raw transcript text or secrets
