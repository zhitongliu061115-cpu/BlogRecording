## ADDED Requirements

### Requirement: Generate Keywords And Tags
The system SHALL generate concise keywords and tags for a podcast session from its transcript or structured summary.

#### Scenario: Structured summary exists
- **WHEN** a session has structured summary data
- **THEN** tag generation uses the structured summary as the preferred context

#### Scenario: Only transcript exists
- **WHEN** a session has transcript text but no structured summary
- **THEN** tag generation can use the transcript text for that session only

### Requirement: Normalize Generated Tags
The system SHALL normalize generated tags before persistence and display.

#### Scenario: Duplicate tags are returned
- **WHEN** the generation result contains duplicate or whitespace-only tags
- **THEN** the app removes empty tags, collapses duplicates, and preserves a deterministic display order

#### Scenario: Tag is too long
- **WHEN** a generated tag exceeds the configured display limit
- **THEN** the app rejects or truncates it according to the generation policy without corrupting other tags

### Requirement: Tag Generation Blocked States
The system SHALL expose clear blocked states when tags cannot be generated.

#### Scenario: Missing API key
- **WHEN** tag generation requires DeepSeek and no API key is configured
- **THEN** the app does not send a request and shows a missing-key blocked state

#### Scenario: No transcript or summary
- **WHEN** a session has no transcript text and no structured summary
- **THEN** tag generation is unavailable and the user sees that content is required
