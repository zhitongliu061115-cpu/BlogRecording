## ADDED Requirements

### Requirement: Structured Summary Sections
The system SHALL store session summaries as structured sections while preserving a displayable summary text.

#### Scenario: Structured summary is generated
- **WHEN** summary generation succeeds with structured content
- **THEN** the session stores overview, key points, action items, open questions, quote candidates, model name, and generated timestamp

#### Scenario: Legacy text summary exists
- **WHEN** a session has only legacy plain-text summary data
- **THEN** the app displays that summary and treats structured sections as absent rather than corrupted

### Requirement: Structured Summary Parsing Fallback
The system SHALL preserve a usable summary when structured parsing fails.

#### Scenario: Model returns malformed structure
- **WHEN** the summary provider returns content that cannot be parsed into the structured schema
- **THEN** the app stores a safe plain-text summary fallback and records a non-sensitive parse failure state

#### Scenario: Model returns partial structure
- **WHEN** only some structured fields can be parsed
- **THEN** the app preserves valid fields and leaves missing optional sections empty

### Requirement: Structured Summary Display
The detail screen SHALL display structured summary sections when available.

#### Scenario: Structured sections are available
- **WHEN** the user opens a summarized session with structured data
- **THEN** the detail screen shows overview, key points, action items, open questions, and quote candidates as separate sections
