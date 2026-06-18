## ADDED Requirements

### Requirement: Speaker labeling respects enabled setting
The app SHALL label speakers using the diarization engine when speaker diarization is enabled and use stable fallback labeling when it is disabled or unavailable.

#### Scenario: Diarization disabled
- **WHEN** speaker diarization is disabled
- **THEN** recognized segments receive fallback speaker labels

### Requirement: Long segments use unstable fallback
The app SHALL label segments longer than 60 seconds as `Speaker 1` with unstable status rather than sending them through diarization.

#### Scenario: Segment exceeds diarization limit
- **WHEN** a segment duration is greater than 60 seconds
- **THEN** the app returns `Speaker 1` with `unstable=true`

### Requirement: Speaker profiles summarize transcript segments
The app SHALL rebuild speaker profiles from transcript segments after new segments are appended.

#### Scenario: Segments contain multiple speakers
- **WHEN** a session has segments with distinct speaker ids
- **THEN** speaker profiles include segment counts and speech duration per speaker
