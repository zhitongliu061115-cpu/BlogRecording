## ADDED Requirements

### Requirement: Imported Sessions Preserve Podcast Session Contract
Imported content sessions SHALL keep the same identity, title, timestamps, transcript state, summary state, and observable detail contract as recorded podcast sessions.

#### Scenario: Imported session appears in session list
- **WHEN** a local media import creates a podcast session
- **THEN** the home screen can display the session title, source type, transcript state, summary state, and processing status

#### Scenario: Imported session detail opens
- **WHEN** the user opens an imported session
- **THEN** the detail screen shows transcript, summary, import status, and source metadata without requiring recording controls
