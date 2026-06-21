## ADDED Requirements

### Requirement: Generate Structured Session Summary
Session summary generation SHALL request and persist structured summary output for one podcast session at a time.

#### Scenario: Eligible session is summarized
- **WHEN** a session has transcript text, is not recording, and a DeepSeek API key is configured
- **THEN** the summary request asks for structured output using only that session's aggregated transcript

#### Scenario: Structured summary retry fails
- **WHEN** a session has a previous successful summary and a structured summary retry fails
- **THEN** the previous successful summary remains available and the retry error is recorded separately

### Requirement: Summary Privacy With Structured Output
Structured summary generation SHALL NOT expose API keys, raw audio, PCM data, private paths, raw transcript text, or raw provider payloads in logs or errors.

#### Scenario: Structured summary request fails
- **WHEN** the structured summary request or parser fails
- **THEN** logs and user-visible errors contain only sanitized provider, status, and reason information
