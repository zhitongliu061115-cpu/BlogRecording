## ADDED Requirements

### Requirement: Main screens and navigation remain available
The app SHALL keep Home, Settings, History, and Detail screens reachable through the existing Compose app flow.

#### Scenario: Navigate from Home to Settings
- **WHEN** the user chooses the settings action on Home
- **THEN** the app displays Settings and returning goes back to Home

#### Scenario: Open recording detail from History
- **WHEN** the user selects a recording in History
- **THEN** the app displays Detail for that recording

### Requirement: Home exposes recording and summary actions
The Home screen SHALL expose internal-audio recording, microphone recording, stop recording, generate summary, history, and settings actions according to current state.

#### Scenario: Start microphone action
- **WHEN** the user chooses microphone recording
- **THEN** the app starts the microphone permission and recording flow

#### Scenario: Generate summary action
- **WHEN** a current recording has transcript text and the user chooses summary generation
- **THEN** the app starts summary generation for the current recording

### Requirement: Privacy notice gates first run
The app SHALL show the first-run privacy notice until `first_run_privacy_accepted` is persisted as true.

#### Scenario: First run privacy not accepted
- **WHEN** settings report `firstRunPrivacyAccepted=false`
- **THEN** the privacy notice is visible and cannot be dismissed by outside tap

#### Scenario: User accepts privacy notice
- **WHEN** the user accepts the privacy notice
- **THEN** the setting is persisted and the notice is no longer shown

### Requirement: Detail supports transcript, summary, copy, and delete actions
The Detail screen SHALL display transcript, summary, and speaker information and keep existing copy, regenerate summary, and delete actions.

#### Scenario: Delete current recording
- **WHEN** the user deletes the current recording from Detail
- **THEN** the app removes the recording and returns to History
