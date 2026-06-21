## Harness

Required commands:

```powershell
openspec.cmd status --change "add-structured-session-summary" --json
openspec.cmd validate add-structured-session-summary
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify summary generation with configured API key, missing API key, empty transcript, malformed model response, and retry after previous success.
- Confirm logs and user-visible errors do not contain API keys, raw transcript text, raw provider payloads, raw audio, or private paths.
