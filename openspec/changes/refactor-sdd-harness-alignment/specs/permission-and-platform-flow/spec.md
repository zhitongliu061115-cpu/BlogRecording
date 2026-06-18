## ADDED Requirements

### Requirement: Microphone flow requests required runtime permissions
The app SHALL request `RECORD_AUDIO` and, on Android 13 or newer, `POST_NOTIFICATIONS` before microphone recording.

#### Scenario: Record audio denied
- **WHEN** the user denies record audio permission
- **THEN** the app reports `RecordAudioPermissionDenied`

#### Scenario: Notification denied on Android 13+
- **WHEN** the user denies notification permission on Android 13 or newer
- **THEN** the app reports `NotificationPermissionDenied`

### Requirement: Internal audio flow requests MediaProjection after runtime permissions
The app SHALL request audio/notification permissions and then launch MediaProjection capture for internal audio.

#### Scenario: MediaProjection denied
- **WHEN** the user denies MediaProjection
- **THEN** the app creates an internal-audio session saved as `ERROR`

### Requirement: Foreground service startup failure is persisted
The app SHALL save a failed recording session when foreground service startup fails.

#### Scenario: Foreground service cannot start
- **WHEN** foreground service startup reports an error
- **THEN** the app saves a session with `ERROR` and exposes the error
