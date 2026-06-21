## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Session Model

- [x] 2.1 Add imported-content source metadata with backward-compatible defaults.
- [x] 2.2 Persist imported sessions without logging SAF URIs, full local paths, or media content.
- [x] 2.3 Add JSON compatibility tests for existing recorded sessions and new imported sessions.

## 3. Local Import Flow

- [x] 3.1 Add a local audio/video selection entry using Android SAF.
- [x] 3.2 Validate selected MIME type, display name, size, and cancellation state.
- [x] 3.3 Copy or open the selected media through app-private access for processing.
- [x] 3.4 Extract audio from supported audio/video containers and reject files without usable audio.

## 4. Transcription Integration

- [x] 4.1 Feed decoded imported audio into the existing transcription chunk flow.
- [x] 4.2 Map import progress and failures into home/detail processing UI state.
- [x] 4.3 Preserve existing live recording behavior while imported media is processing.

## 5. Tests And Harness

- [x] 5.1 Add unit tests for import metadata, format handling, status mapping, and transcription entry.
- [x] 5.2 Run `openspec.cmd validate add-local-media-import`.
- [x] 5.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 5.4 Manually verify mp3, m4a, mp4 with audio, mp4 without audio, read failure, and user cancellation on a device or emulator.

## Suggested Commits

- `文档：新增本地媒体导入规格`
- `功能：新增导入来源元数据`
- `功能：新增本地媒体导入入口`
- `功能：接入本地媒体转写流程`
- `修复：完善本地媒体导入异常提示`
- `测试：补充本地媒体导入用例`
