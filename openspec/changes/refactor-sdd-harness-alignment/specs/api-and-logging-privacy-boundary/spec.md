## ADDED Requirements

### Requirement: DeepSeek endpoint remains stable
The app SHALL call `https://api.deepseek.com/chat/completions` for summary generation unless a later spec changes the provider.

#### Scenario: Summary request is sent
- **WHEN** a summary request is executed
- **THEN** it targets the DeepSeek chat completions endpoint

### Requirement: HTTP and parsing failures are handled safely
The app SHALL map HTTP 401, HTTP 429, other non-2xx responses, network failures, empty content, and malformed content to app errors without exposing secrets.

#### Scenario: Non-2xx response is returned
- **WHEN** DeepSeek returns a non-success response other than 401 or 429
- **THEN** the app reports a network failure with status context only

### Requirement: Logs avoid sensitive payloads
The app SHALL NOT log API keys, full transcripts, raw response bodies, raw audio, PCM chunks, or speaker embeddings.

#### Scenario: Network request fails
- **WHEN** summary generation fails
- **THEN** logs and user-visible messages exclude the API key and sensitive payloads
