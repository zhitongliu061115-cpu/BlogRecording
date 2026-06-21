## ADDED Requirements

### Requirement: Generate Timeline Chapters
The system SHALL generate ordered chapter summaries for a podcast session from timestamped transcript context.

#### Scenario: Transcript has timestamps
- **WHEN** a session has transcript segments with start and end times
- **THEN** the app can generate timeline chapters with approximate start time, end time, title, key points, and source transcript range

#### Scenario: Transcript has no usable timestamps
- **WHEN** transcript segments do not contain usable timing information
- **THEN** the app falls back to untimed sections instead of inventing exact chapter times

### Requirement: Validate Timeline Ranges
The system SHALL validate generated chapter time ranges before displaying or exporting them.

#### Scenario: Chapter range is invalid
- **WHEN** a generated chapter has a reversed, overlapping, negative, or out-of-bounds range
- **THEN** the app corrects it only when safe or omits the invalid timing while preserving the chapter text

### Requirement: Display Timeline Chapters
The detail screen SHALL show timeline chapters when they are available.

#### Scenario: Timeline chapters exist
- **WHEN** the user opens a session with generated timeline chapters
- **THEN** the detail screen displays chapters in chronological order with title, approximate time range, and key points
