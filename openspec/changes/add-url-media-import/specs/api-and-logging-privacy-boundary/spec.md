## ADDED Requirements

### Requirement: URL Import Privacy Boundary
URL import SHALL NOT leak secrets, private local paths, raw media, cookies, or full transcript text through logs, errors, or persisted user-visible fields.

#### Scenario: URL contains sensitive query parameters
- **WHEN** a URL import request includes query parameters
- **THEN** logs and errors redact or omit the query string unless it is known to be non-sensitive

#### Scenario: URL import fails
- **WHEN** a network, parsing, or download failure occurs
- **THEN** diagnostics use sanitized host/status/reason information and do not include cookies, API keys, raw response bodies, private cache paths, raw audio, or full transcript text

### Requirement: URL Import Does Not Use User Credentials
The URL import MVP SHALL NOT request, store, or replay user credentials for third-party podcast services.

#### Scenario: Episode requires login
- **WHEN** a Xiaoyuzhou episode or other URL cannot be resolved without authentication
- **THEN** the app reports that authenticated/private content is unsupported and does not ask for cookies or credentials
