## Harness

Required commands:

```powershell
openspec.cmd status --change "add-session-export" --json
openspec.cmd validate add-session-export
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Run `.\gradlew.bat connectedDebugAndroidTest` when a device or emulator is available.
- Manually verify Markdown, TXT, and JSON export through save and share flows.
- Manually verify cancellation, write failure, sanitized filenames, missing optional sections, and long transcript export.
- Confirm exported files are user-requested artifacts and no raw audio, PCM, API keys, cookies, or private paths are included.
