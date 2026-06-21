## ADDED Requirements

### Requirement: Export Session Content
The app SHALL export one podcast session's content to Markdown, TXT, or JSON.

#### Scenario: Export full session
- **WHEN** the user exports a session with transcript, structured summary, timeline chapters, tags, and favorited highlights
- **THEN** the exported content includes available sections in a deterministic order for the selected format

#### Scenario: Export session with missing optional data
- **WHEN** a session lacks timeline chapters, tags, highlights, or summary
- **THEN** the export succeeds with the available content and does not invent missing sections

### Requirement: Export Via Android System Flows
The app SHALL use Android system save or share flows for export.

#### Scenario: User saves export
- **WHEN** the user chooses a format and confirms a save location
- **THEN** the app writes the selected export content to the chosen destination

#### Scenario: User cancels export
- **WHEN** the user cancels the save or share flow
- **THEN** the app does not persist an error state

### Requirement: Export Error Handling
The app SHALL report export failures without corrupting session data.

#### Scenario: Write fails
- **WHEN** the export destination cannot be written
- **THEN** the app shows a recoverable export error and leaves session data unchanged

### Requirement: Export Privacy Boundary
Exported files SHALL include only user-requested session content and SHALL NOT include API keys, cookies, private cache paths, raw audio, PCM data, or internal debug metadata.

#### Scenario: JSON export is generated
- **WHEN** the user exports JSON
- **THEN** the JSON includes a format version and session content fields without secrets or private internal paths
