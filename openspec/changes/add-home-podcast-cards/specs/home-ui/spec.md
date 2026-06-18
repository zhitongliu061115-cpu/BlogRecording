## ADDED Requirements

### Requirement: Podcast cards on home screen
The Home screen SHALL present each podcast session as an independent card showing its title, session status, total recorded duration, recording segment count, transcription state, and summary state.

#### Scenario: Multiple podcasts are listed independently
- **GIVEN** two or more podcast sessions exist
- **WHEN** the Home screen is displayed
- **THEN** each podcast session appears as its own card with its own title, status, duration, segment count, transcription state, and summary state

### Requirement: Create new podcast session
The Home screen SHALL provide a user action to create a new podcast session before recording.

#### Scenario: Create from home
- **GIVEN** the user is on the Home screen
- **WHEN** the user chooses the create podcast action
- **THEN** a new podcast session is created and appears in the card list as a separate session

### Requirement: Rename podcast session
The Home screen SHALL allow the user to rename a podcast session without affecting its recording segments, transcript, or summary.

#### Scenario: Rename from podcast card
- **GIVEN** a podcast session card is visible
- **WHEN** the user enters a new title and confirms rename
- **THEN** that card shows the new title and the session keeps its existing recording segments, transcript, and summary state

### Requirement: Start recording from card
The Home screen SHALL allow the user to start recording from a podcast card when no other segment is active and the session can record.

#### Scenario: Start first segment from card
- **GIVEN** a podcast session has no active recording segment and no other podcast is recording
- **WHEN** the user starts recording from that podcast card
- **THEN** that podcast card enters Recording state and receives an active recording segment

### Requirement: Pause active recording from card
The Home screen SHALL allow the user to pause the active recording from the recording podcast card.

#### Scenario: Pause active card
- **GIVEN** a podcast session card is Recording
- **WHEN** the user chooses pause on that card
- **THEN** the active segment is ended, the session remains resumable, and the card no longer displays Recording

### Requirement: Resume paused podcast from card
The Home screen SHALL allow the user to resume a paused podcast session by adding a new recording segment.

#### Scenario: Resume paused card
- **GIVEN** a podcast session card is paused and has at least one completed segment
- **WHEN** the user chooses resume on that card
- **THEN** a new recording segment is appended to the same podcast session and that card enters Recording state

#### Scenario: Resume another card while one records
- **GIVEN** podcast A is Recording and podcast B is paused
- **WHEN** the user chooses resume on podcast B
- **THEN** podcast A is paused before podcast B enters Recording state

### Requirement: Finish podcast session
The Home screen SHALL allow the user to finish a podcast session when it is paused or after pausing an active recording.

#### Scenario: Finish paused podcast
- **GIVEN** a podcast session is paused and has at least one completed segment
- **WHEN** the user chooses finish on that card
- **THEN** the session becomes ready for summary or review and no active recording segment remains

#### Scenario: Finish recording podcast
- **GIVEN** a podcast session card is Recording
- **WHEN** the user chooses finish on that card
- **THEN** the active segment is paused first and the session then becomes ready for summary or review

### Requirement: Start summary from card
The Home screen SHALL allow the user to start summary from a podcast card only when the session has transcript text and is not actively recording.

#### Scenario: Start summary with transcript
- **GIVEN** a podcast session has transcript text and is not Recording
- **WHEN** the user chooses start summary on that card
- **THEN** summary generation starts for that podcast session

#### Scenario: Summary blocked while recording
- **GIVEN** a podcast session card is Recording
- **WHEN** the user views the card actions
- **THEN** the start summary action is disabled or requires the user to pause before summary can start

### Requirement: Disable invalid actions
The Home screen SHALL disable actions that are invalid for the card's current UI state.

#### Scenario: No transcript cannot summarize
- **GIVEN** a podcast session has no transcript text
- **WHEN** the Home screen displays that card
- **THEN** the start summary action is disabled

#### Scenario: Another podcast is recording
- **GIVEN** podcast A is Recording and podcast B is not Recording
- **WHEN** the Home screen displays podcast B
- **THEN** podcast B's start or resume action reflects that choosing it will first pause podcast A before recording B

#### Scenario: Only one card displays recording
- **GIVEN** any podcast session is Recording
- **WHEN** the Home screen displays the card list
- **THEN** at most one card is visually marked as Recording

### Requirement: Empty home state
The Home screen SHALL show an empty state with a clear create-session entry when no podcast sessions exist.

#### Scenario: No podcasts exist
- **GIVEN** no podcast sessions exist
- **WHEN** the Home screen is displayed
- **THEN** the Home screen shows an empty state and a create podcast session action
