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
