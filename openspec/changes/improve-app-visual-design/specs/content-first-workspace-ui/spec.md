## ADDED Requirements

### Requirement: Focused Home workflow
The Home screen SHALL present a clear page identity, one primary create or capture area, a session library, and secondary local-model/privacy information without duplicating equivalent actions.

#### Scenario: Empty Home presents a primary next step
- **WHEN** no podcast session exists
- **THEN** Home presents one prominent create action plus distinct microphone, system audio, local media, and URL source choices

#### Scenario: Existing sessions prioritize the library
- **WHEN** podcast sessions exist
- **THEN** Home presents the session list as the primary content and keeps new capture/import actions available without repeating them in multiple sections

### Requirement: Session cards are action-oriented
Session cards SHALL prioritize title, status, duration, transcript progress, and the next valid action while keeping secondary actions visually quieter.

#### Scenario: Recording session is immediately recognizable
- **WHEN** a session is actively recording
- **THEN** its card shows an explicit recording indicator and makes pause or finish more prominent than unavailable actions

#### Scenario: Session card remains compact
- **WHEN** a session has tags, transcript preview, and processing status
- **THEN** the card presents those details without rendering a dense wall of equally weighted buttons

### Requirement: AI workflow separates selection and conversation
The AI screen SHALL clearly separate podcast selection from an active conversation and keep the message composer anchored as the primary interaction when a conversation is active.

#### Scenario: Podcast selection is understandable
- **WHEN** no podcast is selected
- **THEN** the screen presents available podcast cards with title, readiness, and transcript context

#### Scenario: Conversation distinguishes participants
- **WHEN** user and assistant messages are displayed
- **THEN** alignment, surface treatment, and sender semantics clearly distinguish the two participants

### Requirement: Detail screen supports progressive reading
The Detail screen SHALL organize session metadata, summary, chapters, highlights, transcript, QA, and export actions into a clear reading order with destructive actions visually separated from primary actions.

#### Scenario: Summary is easier to reach than raw transcript
- **WHEN** a summarized session is opened
- **THEN** overview and structured summary content appear before the full transcript

#### Scenario: Destructive action is not primary
- **WHEN** the Detail screen is displayed
- **THEN** delete is visually separated from back, summary, copy, export, and share actions

### Requirement: Settings use grouped controls
The Settings screen SHALL group account/API, summary preferences, local processing, model status, and privacy information into labeled sections with suitable control types.

#### Scenario: Binary settings use switches
- **WHEN** VAD or speaker diarization settings are displayed
- **THEN** each binary value is represented by a switch with a readable label and supporting text

#### Scenario: Save action remains clear
- **WHEN** the user edits settings on a compact screen
- **THEN** the save command remains visually prominent and does not compete with navigation or destructive API-key actions

### Requirement: Empty and secondary screens remain useful
History and Mine screens SHALL provide concise status summaries, clear rows for navigation, and intentional empty states rather than isolated placeholder text.

#### Scenario: Empty history explains the state
- **WHEN** there are no historical sessions
- **THEN** History shows an intentional empty state with context and a clear way back

#### Scenario: Mine summarizes local readiness
- **WHEN** Mine is opened
- **THEN** it summarizes session count, DeepSeek configuration, and local processing readiness before presenting Settings and History destinations
