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

## 2026-06-21 Verification

- `openspec.cmd status --change "add-session-export" --json`: passed.
- `openspec.cmd validate add-session-export`: passed.
- `git diff --check`: passed with only the pre-existing unrelated `fix-processing-stage-feedback/harness.md` LF/CRLF warning.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat assembleDebug`: passed.
- `.\gradlew.bat connectedDebugAndroidTest`: not run because `adb` is not available in PATH in this environment.
- Manual save/share/cancellation/write-failure checks: not run in this environment; requires device or emulator system UI.
- Privacy check: renderer tests assert JSON omits API keys and private audio/PCM path fields; full device export inspection remains manual.
