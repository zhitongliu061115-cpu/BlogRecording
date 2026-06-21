## Harness

Required commands:

```powershell
openspec.cmd status --change "fix-settings-entry-access" --json
openspec.cmd validate fix-settings-entry-access
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Targeted verification run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.ui.UiNavigationPolicyTest" --tests "com.example.blogrecording.ui.HomeUiStateMapperTest"
.\gradlew.bat "-Pandroid.testInstrumentationRunnerArguments.class=com.example.blogrecording.ScreenCallbackUiTest" connectedDebugAndroidTest
```

Results:

- `openspec.cmd status --change "fix-settings-entry-access" --json`: passed.
- `openspec.cmd validate fix-settings-entry-access`: passed.
- `git diff --check`: passed.
- Targeted `testDebugUnitTest`: passed.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat assembleDebug`: passed.
- Targeted `connectedDebugAndroidTest`: compiled, but device install failed before running tests with `install-commit Error: -99`.

Manual verification:

- Verify Home with one or more podcast cards shows `历史` and `设置` beside `BlogRecording` before scrolling.
- Tap `设置` from Home and confirm Settings opens.
- Return to Home and confirm History still opens from the header.
