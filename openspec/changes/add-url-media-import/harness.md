## Harness

Required commands:

```powershell
openspec.cmd status --change "add-url-media-import" --json
openspec.cmd validate add-url-media-import
git status --short
git diff --check
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Additional verification:

- Manually verify Xiaoyuzhou URL import with a public episode such as `https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185`.
- Manually verify invalid URL, unsupported URL, direct media URL, RSS enclosure, offline mode, timeout, 401, 429, non-2xx, empty response, oversized file, and non-downloadable resource.
- Confirm no API keys, cookies, full transcript text, raw audio, private cache paths, or sensitive query parameters appear in logs or UI errors.
