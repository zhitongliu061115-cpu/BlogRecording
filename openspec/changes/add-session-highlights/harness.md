## Harness

Required commands:

```powershell
openspec.cmd status --change "add-session-highlights" --json
openspec.cmd validate add-session-highlights
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify favorite, unfavorite, persistence after app restart, regeneration preserving favorites, missing source time, and duplicate candidate handling.
- Confirm highlight logs do not include full transcript text or raw provider payloads.

Results 2026-06-21:

- PASS `openspec.cmd status --change "add-session-highlights" --json`
- PASS `openspec.cmd validate add-session-highlights`
- PASS `git status --short` with unrelated existing dirty file `openspec/changes/fix-processing-stage-feedback/harness.md` and unrelated pending feature change directories present.
- PASS `git diff --check`
- PASS `.\gradlew.bat testDebugUnitTest`
- PASS `.\gradlew.bat assembleDebug`
- NOT RUN manual favorite, unfavorite, persistence after app restart, regeneration preserving favorites, missing source time, and duplicate candidate checks.
