## ADDED Requirements

### Requirement: Summary generation requires encrypted API key and transcript
The app SHALL read the DeepSeek API key from encrypted local storage and require non-blank transcript text before sending a summary request.

#### Scenario: API key missing
- **WHEN** the user requests a summary without a saved API key
- **THEN** the app reports `DeepSeekApiKeyMissing` and does not send a request

#### Scenario: Transcript is blank
- **WHEN** the user requests a summary for a blank transcript
- **THEN** the app stops summary generation and shows an error

### Requirement: Summary requests send transcript text only
The app SHALL send only prompt content derived from transcript text, timestamps, speaker labels, and summary settings to DeepSeek and SHALL NOT send raw audio, PCM chunks, local files, API keys, or speaker embeddings.

#### Scenario: Summary request is built
- **WHEN** the app prepares a DeepSeek request
- **THEN** the request body contains prompt messages derived from transcript/settings only

### Requirement: Long transcripts are summarized in chunks
The app SHALL split long transcripts into partial summaries and merge them into a final recap.

#### Scenario: Transcript exceeds chunk limit
- **WHEN** transcript text exceeds the configured summary chunk size
- **THEN** the app generates partial summaries and then a final combined summary

### Requirement: DeepSeek failures map to app errors
The app SHALL map unauthorized, rate limited, non-success, network, empty-response, and malformed-response failures to app errors.

#### Scenario: DeepSeek returns 401
- **WHEN** DeepSeek responds with HTTP 401
- **THEN** the app reports `DeepSeekUnauthorized`

#### Scenario: DeepSeek returns 429
- **WHEN** DeepSeek responds with HTTP 429
- **THEN** the app reports `DeepSeekRateLimited`
