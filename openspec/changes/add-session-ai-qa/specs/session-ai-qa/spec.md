## ADDED Requirements

### Requirement: Ask Questions About One Session
The app SHALL answer user questions using only the selected podcast session's content.

#### Scenario: User asks about current session
- **WHEN** the user asks a question from a session detail screen with available content and a configured API key
- **THEN** the app sends a QA request grounded only in that session's transcript, summary, timeline, tags, and highlights

#### Scenario: Other sessions exist
- **WHEN** the QA context is built for one session
- **THEN** transcript, summary, tags, highlights, timeline, and QA history from other sessions are excluded

### Requirement: QA Blocked States
The app SHALL block QA before network request when required inputs are missing.

#### Scenario: Missing API key
- **WHEN** the user asks a question without a configured DeepSeek API key
- **THEN** the app does not send a request and shows a missing-key state

#### Scenario: Missing session content
- **WHEN** the selected session has no transcript, summary, timeline, tags, or highlights
- **THEN** the app does not send a request and explains that episode content is required

### Requirement: Persist Session QA History
The app SHALL persist QA history under the podcast session.

#### Scenario: Answer succeeds
- **WHEN** a QA request succeeds
- **THEN** the question, answer, timestamp, status, and model name are stored for that session

#### Scenario: Answer fails
- **WHEN** a QA request fails
- **THEN** the failed question remains retryable with a sanitized error and without deleting previous successful answers

### Requirement: QA Context Truncation
The system SHALL truncate long session context deterministically before sending QA requests.

#### Scenario: Session content is too long
- **WHEN** transcript and recap context exceed the configured prompt budget
- **THEN** the app keeps higher-priority structured recap context and deterministic transcript excerpts within the budget
