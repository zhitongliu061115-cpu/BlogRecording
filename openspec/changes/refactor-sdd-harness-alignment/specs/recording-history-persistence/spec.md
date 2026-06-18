## ADDED Requirements

### Requirement: Recording DataStore contract remains compatible
The app SHALL keep the recording DataStore name `podcast_recap_records` and key families `session_order`, `session_*`, `segments_*`, and `speakers_*`.

#### Scenario: Existing recording data is present
- **WHEN** old session, segment, and speaker keys exist
- **THEN** the app reads them without requiring a migration

### Requirement: Session JSON fields and enum names remain stable
The app SHALL preserve current session JSON field names and enum string names for audio source, recording status, summary style, and summary language.

#### Scenario: Existing session JSON is decoded
- **WHEN** a stored session contains current field names and enum names
- **THEN** the app decodes the session successfully

### Requirement: Session order remains newest first
The app SHALL keep recording history sorted by creation time descending and maintain `session_order` when saving new sessions.

#### Scenario: New session is saved
- **WHEN** a new session is saved
- **THEN** its id is added to `session_order` and history displays newest sessions first

### Requirement: Deleting a session cleans related data
The app SHALL remove session, segment, and speaker profile data when a recording is deleted.

#### Scenario: Session deleted
- **WHEN** a session is deleted
- **THEN** `session_*`, `segments_*`, and `speakers_*` entries for that id are removed

### Requirement: Interrupted sessions recover on startup
The app SHALL mark capturing, VAD detecting, diarizing, transcribing, and summarizing sessions as `ERROR` on startup.

#### Scenario: App starts after interrupted recording
- **WHEN** a stored session is in an interrupted status
- **THEN** the app saves it as `ERROR` with an interruption message
