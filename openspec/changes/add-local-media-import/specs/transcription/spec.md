## ADDED Requirements

### Requirement: Transcribe Imported Media
The system SHALL feed decoded imported media audio into the existing session transcription flow.

#### Scenario: Imported audio is ready
- **WHEN** imported media has been decoded into audio suitable for ASR
- **THEN** the system creates transcript segments for the imported session using the existing transcript ordering rules

#### Scenario: Imported audio is empty
- **WHEN** imported media decodes successfully but contains no meaningful audio samples
- **THEN** the system does not persist hallucinated transcript text and shows an empty-audio import state

### Requirement: Imported Transcription Does Not Affect Active Recording
Imported media transcription SHALL NOT corrupt active live recording session state.

#### Scenario: Another session is recording
- **WHEN** a live recording session is active and an import is requested
- **THEN** the app either blocks the import with a clear state message or schedules it without creating two active recording sessions
