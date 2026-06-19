## ADDED Requirements

### Requirement: Card system-audio recording
The Home screen SHALL make podcast card Start and Resume actions record system audio through MediaProjection.

#### Scenario: Start system audio from card
- **GIVEN** a draft podcast card is visible and no podcast is recording
- **WHEN** the user chooses Start on that card and grants required permissions
- **THEN** the app starts an internal-audio recording segment for that podcast session

#### Scenario: Resume system audio from card
- **GIVEN** a paused podcast card has a completed segment
- **WHEN** the user chooses Resume on that card and grants MediaProjection permission
- **THEN** the app appends a new internal-audio segment to the same podcast session

#### Scenario: Denied system-audio permission
- **GIVEN** a card recording action is waiting for MediaProjection permission
- **WHEN** the user denies or cancels permission
- **THEN** no silent recording starts and the user-visible state explains that system-audio permission is required

### Requirement: Recent Home cards
The Home screen SHALL show only the latest 5 podcast cards by session updated time while preserving all sessions in History.

#### Scenario: More than five podcasts exist
- **GIVEN** more than five podcast sessions exist
- **WHEN** the Home screen is displayed
- **THEN** only the five most recently updated sessions appear as cards

#### Scenario: Older podcasts remain in History
- **GIVEN** a podcast session is older than the latest five Home cards
- **WHEN** the user opens History
- **THEN** the older podcast remains available there

### Requirement: Card transcript preview
Each podcast card SHALL show a bounded scrollable preview of recent non-empty transcript snippets.

#### Scenario: Card has transcript snippets
- **GIVEN** a podcast session has transcript segments with text
- **WHEN** the Home screen displays that podcast card
- **THEN** the card shows recent transcript snippets in a bounded scroll area

#### Scenario: Card has no transcript snippets
- **GIVEN** a podcast session has no non-empty transcript segments
- **WHEN** the Home screen displays that podcast card
- **THEN** the card shows a concise empty transcript preview state
