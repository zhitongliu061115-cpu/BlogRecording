## Harness

Required commands:

```powershell
openspec.cmd status --change "add-keyword-tag-generation" --json
openspec.cmd validate add-keyword-tag-generation
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify Chinese content, English content, short content, duplicate tags, overlong tags, missing API key, empty transcript, and retry failure.
- Confirm tags do not reveal raw transcript excerpts beyond concise topic labels.

Results 2026-06-21:

- PASS `openspec.cmd status --change "add-keyword-tag-generation" --json`
- PASS `openspec.cmd validate add-keyword-tag-generation`
