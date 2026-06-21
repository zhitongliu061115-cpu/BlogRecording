## Harness

Required commands:

```powershell
openspec.cmd status --change "add-timeline-chapter-summary" --json
openspec.cmd validate add-timeline-chapter-summary
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify long episode content, multi-segment recordings, missing timestamps, invalid model-returned times, and malformed timeline response.
- Confirm timeline errors do not delete transcript or previous summary data.

Results 2026-06-21:

- PASS `openspec.cmd status --change "add-timeline-chapter-summary" --json`
- PASS `openspec.cmd validate add-timeline-chapter-summary`
- PASS `git status --short` with unrelated existing dirty file `openspec/changes/fix-processing-stage-feedback/harness.md` and unrelated pending feature change directories present.
- PASS `git diff --check`
- PASS `.\gradlew.bat testDebugUnitTest`
- PASS `.\gradlew.bat assembleDebug`
- NOT RUN manual long content, multi-segment recording, missing timestamp, invalid timeline, and malformed response checks.
