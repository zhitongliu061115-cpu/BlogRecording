# Tasks

- [x] 1. Add OpenSpec requirements for system-audio card actions, latest-card Home policy, and card transcript preview.
- [x] 2. Add failing tests for latest Home cards, card transcript preview, and internal-audio segment control.
- [x] 3. Implement card-based system-audio start/resume and internal-audio segment orchestration.
- [x] 4. Implement latest 5 Home cards and scrollable transcript previews.
- [x] 5. Update Android UI tests for current Home text and card flow.
- [x] 6. Run Harness checks and record results.

## Harness Results

- `git status --short`: passed before final harness note update.
- `git diff --check`: passed.
- `openspec.cmd status --change fix-home-card-internal-recording --json`: passed.
- `openspec.cmd validate fix-home-card-internal-recording`: passed.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat :app:assembleDebug`: passed.
- `.\gradlew.bat :app:lintDebug`: passed.
- `.\gradlew.bat assembleDebugAndroidTest`: passed.
- `.\gradlew.bat connectedDebugAndroidTest`: environment-blocked on device `PJD110 - Android 16`; Gradle reached the connected device but failed while installing `app-debug-androidTest.apk` with `package install-commit ... Error: -99`, so 0 tests started.
