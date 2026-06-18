## ADDED Requirements

### Requirement: Debug build verifies bundled models
The build SHALL run `verifyBundledModels` before `preBuild` and fail when required model assets are missing or empty.

#### Scenario: Model asset missing
- **WHEN** a required bundled model asset is absent or zero bytes
- **THEN** `assembleDebug` fails before producing an APK

### Requirement: Native sherpa dependency remains available
The app SHALL keep the existing sherpa-onnx AAR dependency available to the Android app module.

#### Scenario: Gradle dependencies resolve
- **WHEN** the app module is configured
- **THEN** the local sherpa-onnx AAR dependency is included

### Requirement: Model assets are not replaced by placeholders
The app SHALL require real non-empty model assets at the configured paths for debug assembly.

#### Scenario: Placeholder model remains
- **WHEN** a required model file is empty
- **THEN** the build fails with model details
