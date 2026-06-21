## ADDED Requirements

### Requirement: URL Import Reuses Imported Content Pipeline
Resolved URL media SHALL enter the same imported-content session and transcription pipeline as local media import.

#### Scenario: URL media download succeeds
- **WHEN** a remote media file is downloaded into private cache
- **THEN** the system treats it as imported content and uses the shared media validation, decoding, transcription, and session display behavior

#### Scenario: URL import creates source metadata
- **WHEN** a URL import creates a podcast session
- **THEN** the persisted session includes sanitized URL source metadata and never stores API keys, cookies, or private cache paths as display text
