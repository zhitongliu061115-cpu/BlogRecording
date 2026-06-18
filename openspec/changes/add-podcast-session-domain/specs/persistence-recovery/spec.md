## ADDED Requirements

### Requirement: Backward compatibility with existing records
The system SHALL keep existing recording history readable after introducing podcast sessions and recording segments.

#### Scenario: Read existing single-record session
- **GIVEN** existing recording history was saved before podcast sessions supported multiple recording segments
- **WHEN** the app reads recording history after this change
- **THEN** the existing record is available as a podcast session with its title, transcript, summary, status, timestamps, source metadata, transcript segments, and speaker information preserved

#### Scenario: Existing transcript segments remain associated
- **GIVEN** an existing record has transcript segments and speaker information
- **WHEN** the record is read as a podcast session
- **THEN** those transcript segments and speaker details remain associated with the same session identity

#### Scenario: Existing completed record can still be summarized
- **GIVEN** an existing completed record has transcript text and no summary
- **WHEN** the record is read as a podcast session
- **THEN** the session can be treated as eligible for future summary behavior by later changes without requiring the user to re-record

### Requirement: Recovery after process restart
The system SHALL recover persisted podcast sessions and recording segments after process restart, including interrupted active states.

#### Scenario: Restore session list after restart
- **GIVEN** multiple podcast sessions and recording segments were persisted
- **WHEN** the process restarts and the app reads stored data
- **THEN** the session list and each session detail are restored with the same identifiers, statuses, titles, segment counts, transcript state, and summary state

#### Scenario: Interrupted recording becomes recoverable paused or error state
- **GIVEN** a podcast session was recording when the process stopped unexpectedly
- **WHEN** the app starts again and performs recovery
- **THEN** the session is no longer treated as actively recording and is persisted in a recoverable paused or error state with existing segments preserved

#### Scenario: Incomplete segment metadata is preserved for diagnosis
- **GIVEN** a recording segment was created but not fully finalized before interruption
- **WHEN** the app recovers session data
- **THEN** the session retains enough segment metadata to show that the segment was interrupted and does not lose completed transcript data from earlier segments

### Requirement: Durable session detail observation
The system SHALL expose observable session lists and session detail data so callers can react to persisted session, segment, transcript, summary, and status changes.

#### Scenario: Observe session list changes
- **GIVEN** a caller observes podcast sessions
- **WHEN** a session is created, renamed, or has its status updated
- **THEN** the observer receives an updated session list reflecting the persisted change

#### Scenario: Observe session detail changes
- **GIVEN** a caller observes one podcast session detail
- **WHEN** a recording segment or transcript segment is appended to that session
- **THEN** the observer receives updated detail for that session without including unrelated sessions
