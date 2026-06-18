## ADDED Requirements

### Requirement: Aggregate transcripts by podcast session
The system SHALL aggregate transcript segments for a podcast session in recording-segment order and transcript time order.

#### Scenario: Aggregate multiple segments
- **GIVEN** a podcast session contains multiple recording segments with transcript segments
- **WHEN** the session aggregate transcript is built
- **THEN** transcript text is ordered by recording segment index and then by transcript start time

#### Scenario: Ignore other sessions
- **GIVEN** two podcast sessions each have transcript segments
- **WHEN** the aggregate transcript for one session is built
- **THEN** only transcript segments belonging to that podcast session are included

### Requirement: Handle partial transcript
The system SHALL allow summary generation from completed transcript text while clearly handling segments that are still missing or failed.

#### Scenario: Some segments have no transcript yet
- **GIVEN** a podcast session has completed transcript text for one segment and another segment has no transcript text yet
- **WHEN** the session aggregate transcript is built
- **THEN** the completed transcript text remains available and the missing segment does not add blank text to the aggregate

#### Scenario: Failed segment does not remove prior transcript
- **GIVEN** a podcast session has previous transcript text and a later segment failed transcription
- **WHEN** the session aggregate transcript is built
- **THEN** previous transcript text remains available and the failed segment is represented by status, not by deleting prior transcript text

### Requirement: Prevent leaking API key or raw audio
The transcription aggregation path SHALL NOT log DeepSeek API keys, raw audio, PCM data, full local audio paths, or full transcript text.

#### Scenario: Aggregation failure logging
- **GIVEN** transcript aggregation encounters missing or failed segment metadata
- **WHEN** logs or user-visible errors are produced
- **THEN** logs and errors do not contain API keys, raw audio, PCM data, full local audio paths, or full transcript text
