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

## 2026-06-21 Verification

- `openspec.cmd status --change "add-session-ai-qa" --json`: passed.
- `openspec.cmd validate add-session-ai-qa`: passed.
- `git diff --check`: passed with only the pre-existing unrelated `fix-processing-stage-feedback/harness.md` LF/CRLF warning.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat assembleDebug`: passed.
- Manual configured-key/no-key/no-content/long-transcript/network-error/retry/follow-up verification: not run in this environment because it requires a device or emulator and a configured real DeepSeek API key.
- Privacy check: unit tests cover request-body API-key exclusion, context single-session selection, deterministic truncation, and sanitized provider errors; full runtime log inspection remains manual.
