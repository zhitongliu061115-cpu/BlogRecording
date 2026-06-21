## MODIFIED Requirements

### Requirement: Main screens and navigation remain available
The app SHALL keep Home, Settings, History, and Detail screens reachable through the existing Compose app flow. The Home screen SHALL expose the Settings action in the Home header or first viewport so users can open Settings without scrolling through podcast cards.

#### Scenario: Navigate from Home to Settings
- **WHEN** the user chooses the settings action on Home
- **THEN** the app displays Settings and returning goes back to Home

#### Scenario: Settings remains visible when Home has podcast cards
- **GIVEN** one or more podcast cards are displayed on Home
- **WHEN** the Home screen is first displayed
- **THEN** the settings action is available before the user scrolls through the card list

#### Scenario: Open recording detail from History
- **WHEN** the user selects a recording in History
- **THEN** the app displays Detail for that recording
