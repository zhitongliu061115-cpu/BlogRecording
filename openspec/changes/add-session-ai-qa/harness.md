## Harness

Required commands:

```powershell
openspec.cmd status --change "add-session-ai-qa" --json
openspec.cmd validate add-session-ai-qa
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify configured API key, missing API key, no transcript/content, long transcript truncation, timeout, 401, 429, non-2xx, empty response, retry, and follow-up history.
- Confirm QA logs do not include API keys, raw prompt context, full question/answer bodies, raw transcript text, raw audio, cookies, or private paths.
