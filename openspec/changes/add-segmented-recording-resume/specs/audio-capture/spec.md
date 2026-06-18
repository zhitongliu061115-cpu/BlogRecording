## ADDED Requirements

### Requirement: Pause active recording segment
The system SHALL pause the active recording by ending only the current recording segment while keeping its podcast session resumable.

#### Scenario: Pause microphone recording segment
- **GIVEN** a podcast session is actively recording from the microphone
- **WHEN** the user pauses recording
- **THEN** the current microphone segment is finalized, the session becomes paused, and the session remains available for resumed recording

#### Scenario: Pause system audio recording segment
- **GIVEN** a podcast session is actively recording system audio
- **WHEN** the user pauses recording
- **THEN** the current system-audio segment is finalized, the session becomes paused, and the MediaProjection capture is stopped for that segment

### Requirement: Resume existing podcast session
The system SHALL resume an existing paused podcast session by creating a new recording segment instead of overwriting or reopening the previous segment.

#### Scenario: Resume microphone session
- **GIVEN** a paused podcast session contains a completed microphone segment
- **WHEN** the user resumes that session with microphone recording
- **THEN** the system creates a new microphone segment associated with the same session and marks that segment active

#### Scenario: Resume system audio session
- **GIVEN** a paused podcast session contains a completed system-audio segment
- **WHEN** the user resumes that session with valid system-audio permission
- **THEN** the system creates a new system-audio segment associated with the same session and marks that segment active

#### Scenario: MediaProjection permission invalid on resume
- **GIVEN** a paused podcast session is resumed for system audio
- **WHEN** MediaProjection permission is denied, cancelled, or invalid
- **THEN** no silent recording starts, the session remains recoverable, and the user-visible state explains that system-audio permission is required

### Requirement: Foreground service notification state
The foreground service notification SHALL reflect the active podcast title, capture source, and recording state while a segment is active.

#### Scenario: Notification while recording
- **GIVEN** a podcast session named "Episode A" is actively recording a segment
- **WHEN** the foreground service notification is shown
- **THEN** the notification identifies "Episode A" and shows that recording is active

#### Scenario: Notification after pause
- **GIVEN** a podcast session has just paused its active segment
- **WHEN** the foreground service state is updated
- **THEN** the notification no longer claims an active recording segment is running

#### Scenario: Notification actions
- **GIVEN** a podcast session is actively recording
- **WHEN** the notification exposes recording controls
- **THEN** pause and finish actions target the active session and do not start a second active segment
