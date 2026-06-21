## ADDED Requirements

### Requirement: Home Card Shows Generated Tags
The Home screen SHALL display generated session tags compactly when they are available.

#### Scenario: Session has tags
- **WHEN** a podcast session has generated tags
- **THEN** its home card shows a compact subset of tags without hiding primary recording or summary actions

#### Scenario: Session has no tags
- **WHEN** a podcast session has no generated tags
- **THEN** the home card remains usable and does not show an error solely because tags are absent
