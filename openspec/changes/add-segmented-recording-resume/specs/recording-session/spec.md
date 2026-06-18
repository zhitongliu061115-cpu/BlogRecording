## ADDED Requirements

### Requirement: Switch active podcast session
The system SHALL switch recording from one podcast session to another by pausing the current active segment before starting a new segment on the target session.

#### Scenario: Switch from podcast A to podcast B
- **GIVEN** podcast A is actively recording and podcast B exists
- **WHEN** the user starts or resumes recording on podcast B
- **THEN** podcast A's active segment is paused before podcast B receives a new active segment

#### Scenario: Switch back to podcast A
- **GIVEN** podcast B is actively recording and podcast A is paused
- **WHEN** the user resumes podcast A
- **THEN** podcast B's active segment is paused and podcast A receives a new active segment

### Requirement: Single active recording invariant
The system SHALL ensure that at most one recording segment is active across all podcast sessions at any time.

#### Scenario: Double start request
- **GIVEN** no recording segment is active
- **WHEN** two start requests are issued before the first one finishes
- **THEN** the system starts at most one segment and rejects or ignores the duplicate request with a deterministic result

#### Scenario: Resume while another session records
- **GIVEN** one podcast session is actively recording
- **WHEN** another session is resumed
- **THEN** the first active segment is paused before the second segment becomes active

### Requirement: Recovery after interrupted active recording
The system SHALL recover interrupted active recordings after process death or service termination.

#### Scenario: App process killed while recording
- **GIVEN** a podcast session has an active recording segment
- **WHEN** the app process restarts
- **THEN** the session is no longer marked actively recording, the interrupted segment is preserved, and the session can be resumed or reviewed

#### Scenario: Service killed before segment metadata is complete
- **GIVEN** a recording segment was created but not finalized before service termination
- **WHEN** recovery runs
- **THEN** the session preserves completed segments and marks the incomplete segment as interrupted or error without deleting prior transcript data

### Requirement: Finalize podcast session
The system SHALL allow a paused podcast session to be finalized when the user confirms no more recording segments will be added.

#### Scenario: Finish paused session
- **GIVEN** a podcast session is paused and has at least one completed segment
- **WHEN** the user finishes the session
- **THEN** the session becomes ready for summary or review and no active segment remains

#### Scenario: Finish while recording
- **GIVEN** a podcast session is actively recording
- **WHEN** the user finishes the session
- **THEN** the system first pauses the active segment and then finalizes the session
