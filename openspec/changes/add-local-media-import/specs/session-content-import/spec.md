## ADDED Requirements

### Requirement: Imported Content Session Source
The system SHALL represent imported content as a podcast session source with sanitized metadata.

#### Scenario: Create session from imported content
- **WHEN** a supported local media file is accepted for import
- **THEN** the system creates or updates one podcast session with imported-content source metadata, a stable session identifier, and no user-visible full local path

#### Scenario: Preserve recorded sessions
- **WHEN** existing recorded sessions are loaded after import support is added
- **THEN** the sessions remain readable with their previous recording source, transcript, summary, and status data preserved

### Requirement: Import Processing State
The system SHALL expose import progress and import failures through the same user-facing processing state family used by sessions.

#### Scenario: Import is processing
- **WHEN** a selected media file is being copied, decoded, or prepared for transcription
- **THEN** the home card and detail screen show that import processing is in progress

#### Scenario: Import fails before transcription
- **WHEN** media validation, reading, or decoding fails before ASR starts
- **THEN** the session shows a recoverable import error without deleting existing transcript or summary content

### Requirement: Import Privacy Boundary
The import path SHALL NOT log or display raw media content, PCM buffers, full device paths, SAF URI strings, API keys, or full transcript text.

#### Scenario: Import error is reported
- **WHEN** an import operation fails
- **THEN** user-visible errors and logs include only sanitized reason codes or short messages
