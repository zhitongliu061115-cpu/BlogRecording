## Harness

Required commands:

```powershell
openspec.cmd status --change "add-local-media-import" --json
openspec.cmd validate add-local-media-import
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Run `.\gradlew.bat connectedDebugAndroidTest` when a device or emulator is available.
- Manually verify SAF file selection, user cancellation, mp3 import, m4a import, mp4 with audio, mp4 without audio, unsupported media, and read failure.
- Confirm logs and errors do not contain API keys, full file paths, SAF URI strings, raw audio, PCM, or transcript content.
