## ADDED Requirements

### Requirement: Usage-based internal playback capture by default
The app SHALL default internal-audio capture to Android playback usage filters instead of automatically restricting capture to an installed preferred package UID.

#### Scenario: Active playback app differs from installed preferred package
- **WHEN** an installed preferred package is not the active playback source
- **THEN** internal recording still builds a capture configuration from media/game/unknown usages instead of filtering to that package UID

#### Scenario: Explicit UID targets are supplied
- **WHEN** the capture manager is constructed with explicit preferred capture UIDs
- **THEN** it may build a matching-UID capture configuration for that explicit request

### Requirement: Usage capture diagnostics
The app SHALL log whether the internal capture configuration uses usage filters or explicit UID filters without logging audio content.

#### Scenario: Usage filters are used
- **WHEN** internal recording starts with the default MVP policy
- **THEN** logs indicate usage-based capture and list only usage constants

#### Scenario: Non-zero PCM arrives
- **WHEN** internal capture first reads non-zero PCM
- **THEN** logs indicate the first non-zero average amplitude without logging raw samples or transcript text
