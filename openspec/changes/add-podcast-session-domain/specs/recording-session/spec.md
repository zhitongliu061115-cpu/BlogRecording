## ADDED Requirements

### Requirement: Podcast session creation
The system SHALL allow multiple podcast sessions to exist independently, each with a title, source metadata, timestamps, status, transcript state, summary state, and recording segment membership.

#### Scenario: Create the first podcast session
- **GIVEN** no podcast sessions exist
- **WHEN** a new podcast session is created
- **THEN** the system persists one session with a stable identifier, default title, creation time, update time, and a non-recording initial status

#### Scenario: Create multiple podcast sessions
- **GIVEN** one podcast session already exists
- **WHEN** another podcast session is created
- **THEN** the system persists both sessions as separate records with separate identifiers and no shared recording segments

### Requirement: Multiple recording segments per session
The system SHALL allow a podcast session to contain zero or more recording segments, and each recording segment SHALL belong to exactly one podcast session.

#### Scenario: Append the first segment
- **GIVEN** a podcast session exists without recording segments
- **WHEN** a recording segment is appended to that session
- **THEN** the session detail includes that segment and the session segment count reflects one segment

#### Scenario: Append multiple segments to one session
- **GIVEN** a podcast session already contains one recording segment
- **WHEN** another recording segment is appended to the same session
- **THEN** the session detail includes both segments in chronological order

#### Scenario: Segments do not leak between sessions
- **GIVEN** two podcast sessions exist
- **WHEN** a recording segment is appended to the first session
- **THEN** the second session detail does not include that segment

### Requirement: Session rename
The system SHALL allow a podcast session title to be renamed without changing its identifier, segments, transcript, summary, or status.

#### Scenario: Rename existing session
- **GIVEN** a podcast session exists with a title and recording segments
- **WHEN** the session is renamed
- **THEN** the system persists the new title and preserves the same identifier, segment list, transcript state, summary state, and status

#### Scenario: Rename does not affect other sessions
- **GIVEN** two podcast sessions exist
- **WHEN** the first session is renamed
- **THEN** the second session title and details remain unchanged

### Requirement: Session status lifecycle
The system SHALL persist a podcast session status that can represent draft, recording, paused, processing, ready-for-summary, summarizing, summarized, and error outcomes.

#### Scenario: Start recording from draft
- **GIVEN** a podcast session is in a draft state
- **WHEN** recording starts for that session
- **THEN** the session status becomes recording

#### Scenario: Pause recording without completing session
- **GIVEN** a podcast session is recording
- **WHEN** the active recording segment is paused
- **THEN** the session status becomes paused and remains eligible for later resumed recording

#### Scenario: Mark ready for summary
- **GIVEN** a podcast session is paused and contains finalized transcript text
- **WHEN** the session is finalized for review
- **THEN** the session status becomes ready-for-summary

#### Scenario: Mark summarized
- **GIVEN** a podcast session is ready for summary
- **WHEN** a session summary is successfully persisted
- **THEN** the session status becomes summarized and the summary content remains associated with that session

#### Scenario: Mark error
- **GIVEN** a podcast session is in any lifecycle state
- **WHEN** a recoverable domain or persistence error is recorded
- **THEN** the session status becomes error and the previous segments and transcript state remain available for recovery or review
