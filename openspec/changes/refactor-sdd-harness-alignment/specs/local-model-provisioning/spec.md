## ADDED Requirements

### Requirement: Bundled model paths are fixed assets
The app SHALL use the existing bundled model asset paths for SenseVoice, tokens, VAD, diarization segmentation, and diarization embedding.

#### Scenario: App starts with bundled models
- **WHEN** bundled model assets exist
- **THEN** the installer copies them to the private `files/bundled_models` directory

### Requirement: Model status reports loaded or missing
The app SHALL report model status for SenseVoice, VAD, and diarization based on copied private files.

#### Scenario: Required private model file missing
- **WHEN** a private copied model file is absent or empty
- **THEN** the corresponding model status is `MISSING`

### Requirement: Settings do not allow manual model path entry
The app SHALL keep model paths managed by the bundled installer and SHALL NOT expose user-editable model path inputs.

#### Scenario: User saves settings
- **WHEN** Settings are saved
- **THEN** model paths are overwritten with installer-managed paths
