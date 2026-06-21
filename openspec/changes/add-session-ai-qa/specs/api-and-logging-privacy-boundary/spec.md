## ADDED Requirements

### Requirement: QA Privacy Boundary
Session QA SHALL NOT expose API keys, raw prompt context, full transcript text, full question/answer bodies, raw audio, PCM data, cookies, or private paths through logs or user-visible errors.

#### Scenario: QA request starts
- **WHEN** a QA request is sent
- **THEN** logs may include sanitized session identifier, model name, and state transitions but not raw prompt context or API key

#### Scenario: QA request fails
- **WHEN** a QA network or provider error occurs
- **THEN** logs and user-visible errors include sanitized status/reason information only

### Requirement: QA Provider Scope
Session QA SHALL send episode content only to the configured summary/QA provider endpoint.

#### Scenario: QA request is made
- **WHEN** the app sends a QA request
- **THEN** only the configured DeepSeek chat completions endpoint receives the session context and user question
