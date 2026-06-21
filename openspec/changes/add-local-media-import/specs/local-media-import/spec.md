## ADDED Requirements

### Requirement: Select Local Audio Or Video
The app SHALL allow the user to choose one local audio or video file through Android system file selection.

#### Scenario: User selects a supported audio file
- **WHEN** the user selects a supported audio file
- **THEN** the app accepts the selection and begins import processing for that file

#### Scenario: User cancels selection
- **WHEN** the user opens the file picker and cancels without selecting a file
- **THEN** no session is created and no error state is persisted

### Requirement: Validate Local Media
The app SHALL validate selected local media before transcription starts.

#### Scenario: Unsupported file
- **WHEN** the selected file has an unsupported type or cannot be decoded
- **THEN** the app stops import processing and shows an unsupported-media error

#### Scenario: Video has no audio track
- **WHEN** the selected video file does not contain a usable audio track
- **THEN** the app stops import processing and explains that no audio track was found

### Requirement: Import Local Media Into Private Processing
The app SHALL process selected local media through app-private access without requiring users to manage raw import files.

#### Scenario: Media read succeeds
- **WHEN** the app can read the selected media through SAF
- **THEN** decoded audio is prepared for transcription without exposing the source URI in UI or logs

#### Scenario: Media read fails
- **WHEN** SAF access is revoked or the selected file cannot be read
- **THEN** the app shows a recoverable read-failed state and does not start ASR
