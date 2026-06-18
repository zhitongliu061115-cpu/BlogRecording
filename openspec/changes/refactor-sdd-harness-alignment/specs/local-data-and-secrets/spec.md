## ADDED Requirements

### Requirement: API key storage contract remains compatible
The app SHALL keep SharedPreferences name `deep_seek_key_store`, keys `iv` and `cipher_text`, Keystore alias `podcast_recap_deep_seek_api_key`, and transformation `AES/GCM/NoPadding`.

#### Scenario: Existing encrypted API key is present
- **WHEN** `iv` and `cipher_text` exist and the Keystore key is available
- **THEN** the app can read the API key

### Requirement: Blank API keys are rejected
The app SHALL reject blank or whitespace-only API keys.

#### Scenario: User saves blank key
- **WHEN** the API key input is blank after trimming
- **THEN** the app returns `DeepSeekApiKeyMissing`

### Requirement: API key delete removes encrypted payload
The app SHALL delete both `iv` and `cipher_text` when the user removes the API key.

#### Scenario: User deletes key
- **WHEN** the user deletes the saved API key
- **THEN** `hasApiKey` becomes false

### Requirement: Secrets are not logged or committed
The app and repository workflow SHALL avoid logging or committing API keys, tokens, certificates, signing files, local configuration, or user data.

#### Scenario: Harness checks repository status
- **WHEN** changes are prepared for commit
- **THEN** ignored local files and sensitive user artifacts are not staged
