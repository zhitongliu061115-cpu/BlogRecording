## ADDED Requirements

### Requirement: Summary eligibility
The system SHALL determine whether a podcast session can be summarized from session-level recording, transcript, and summary state.

#### Scenario: Ready when transcript exists and not recording
- **GIVEN** a podcast session has transcript text and no active recording segment
- **WHEN** summary eligibility is evaluated
- **THEN** the session is eligible to start summary

#### Scenario: Not ready without transcript
- **GIVEN** a podcast session has no transcript text
- **WHEN** summary eligibility is evaluated
- **THEN** the session is not eligible to start summary and the user-visible state explains that transcript text is required

#### Scenario: Not ready while recording
- **GIVEN** a podcast session has an active recording segment
- **WHEN** summary eligibility is evaluated
- **THEN** the session is not eligible for final summary until recording is paused or finished

### Requirement: Start session summary
The system SHALL start summary generation for a podcast session using only that session's aggregated transcript text.

#### Scenario: Start summary for eligible session
- **GIVEN** a podcast session is eligible for summary and a DeepSeek API key is configured
- **WHEN** the user starts summary for that session
- **THEN** the session summary state becomes Summarizing and the DeepSeek request uses the aggregated transcript for that session

### Requirement: Retry failed summary
The system SHALL allow the user to retry summary generation after a previous summary attempt failed.

#### Scenario: Retry failed summary
- **GIVEN** a podcast session has summary status Failed and still has transcript text
- **WHEN** the user retries summary
- **THEN** the session summary state becomes Summarizing and a new DeepSeek request is started for the same aggregated transcript

### Requirement: Preserve previous summary
The system SHALL preserve the last successful summary while a retry is running or fails.

#### Scenario: Failed retry keeps previous summary
- **GIVEN** a podcast session has an existing successful summary
- **WHEN** the user retries summary and the retry fails
- **THEN** the previous summary text remains available and the failed retry error is recorded separately

### Requirement: Handle missing API key
The system SHALL fail summary start with a clear recoverable error when no DeepSeek API key is configured.

#### Scenario: Missing API key
- **GIVEN** a podcast session is otherwise eligible for summary
- **WHEN** the user starts summary without a configured DeepSeek API key
- **THEN** no DeepSeek request is sent and the user-visible state explains that the DeepSeek API key is missing

### Requirement: Prevent leaking API key or raw audio
The system SHALL NOT expose DeepSeek API keys, raw audio, PCM data, full local audio paths, or raw transcript text in logs or errors while generating summaries.

#### Scenario: Summary request logging
- **GIVEN** summary generation starts or fails
- **WHEN** logs or user-visible errors are produced
- **THEN** logs and errors do not contain API keys, raw audio, PCM data, full local audio paths, or full transcript text

#### Scenario: Summary provider scope
- **GIVEN** a podcast session summary is generated
- **WHEN** network requests are made for summary
- **THEN** only the configured DeepSeek chat completions endpoint receives summary request content
