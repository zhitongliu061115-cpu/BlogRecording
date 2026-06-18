## ADDED Requirements

### Requirement: Segment-level transcription
The system SHALL associate transcript output with the recording segment that produced it while preserving session-level transcript aggregation.

#### Scenario: Transcript from resumed segment
- **GIVEN** a podcast session contains one completed segment and one active resumed segment
- **WHEN** ASR produces transcript output for the active resumed segment
- **THEN** the transcript segment is associated with that recording segment and included in the session transcript in chronological order

#### Scenario: Transcript aggregation across segments
- **GIVEN** a podcast session contains multiple completed recording segments with transcript text
- **WHEN** the session detail is loaded
- **THEN** the transcript text is aggregated across all segments in recording order

### Requirement: Segment transcription failure isolation
The system SHALL preserve the podcast session and completed segments when transcription fails for one segment.

#### Scenario: Segment ASR failure
- **GIVEN** a podcast session has previous completed transcript data
- **WHEN** ASR fails for the current segment
- **THEN** the current segment records an error and the session keeps prior segments and transcript data available

#### Scenario: Blank segment transcript
- **GIVEN** a segment contains no recognized speech
- **WHEN** transcription completes with blank output
- **THEN** the segment is retained without adding blank transcript text to the session aggregate

### Requirement: Prevent non-summary audio or transcript uploads
The system SHALL keep audio and segment transcription local and SHALL NOT send raw audio, PCM, or full transcript text to any non-DeepSeek summary endpoint.

#### Scenario: Local segment transcription
- **GIVEN** a recording segment is ready for ASR
- **WHEN** transcription runs
- **THEN** audio and PCM data remain on device and only local ASR components process the segment

#### Scenario: Segment failure logging
- **GIVEN** transcription fails for a segment
- **WHEN** the error is logged or surfaced
- **THEN** logs do not include API keys, full local audio paths, raw PCM data, or full transcript text
