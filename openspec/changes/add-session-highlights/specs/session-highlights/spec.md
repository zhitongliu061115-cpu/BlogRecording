## ADDED Requirements

### Requirement: Generate Session Highlight Candidates
The system SHALL generate highlight or quote candidates for a single podcast session.

#### Scenario: Structured summary has quote candidates
- **WHEN** a structured summary includes quote candidates
- **THEN** the app can create highlight candidates from those quote candidates with source metadata when available

#### Scenario: Transcript fallback is used
- **WHEN** no structured quote candidates exist but transcript text exists
- **THEN** the app can generate highlight candidates from that session's transcript only

### Requirement: Favorite Highlights
The app SHALL allow the user to favorite and unfavorite session highlights.

#### Scenario: User favorites a highlight
- **WHEN** the user marks a highlight as favorite
- **THEN** the highlight favorite state is persisted for that session

#### Scenario: User unfavorites a highlight
- **WHEN** the user removes favorite state from a highlight
- **THEN** the highlight remains available as a candidate unless removed by future generation policy

### Requirement: Preserve Favorites Across Regeneration
The system SHALL preserve user-favorited highlights when generated highlight candidates are refreshed.

#### Scenario: Highlight candidates are regenerated
- **WHEN** summary or highlight generation runs again for a session
- **THEN** previously favorited highlights remain favorited unless the user explicitly removed them

### Requirement: Display Highlights In Detail
The detail screen SHALL show highlight candidates and favorite state.

#### Scenario: Highlights exist
- **WHEN** the user opens a session with highlights
- **THEN** the detail screen displays highlight text, favorite state, and available source time information
