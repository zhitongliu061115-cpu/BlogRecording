## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Export Rendering

- [ ] 2.1 Define export input model and supported formats: Markdown, TXT, and JSON.
- [ ] 2.2 Render transcript, structured summary, timeline chapters, generated tags, and favorited highlights in deterministic order.
- [ ] 2.3 Add filename sanitization and export format versioning.

## 3. Android Save And Share

- [ ] 3.1 Add detail-screen export entry and format picker.
- [ ] 3.2 Implement system save flow for selected export format.
- [ ] 3.3 Implement system share flow for selected export format.
- [ ] 3.4 Handle empty content, write failure, invalid filename, and user cancellation.

## 4. Tests And Harness

- [ ] 4.1 Add unit tests for Markdown, TXT, JSON, escaping, empty fields, filename sanitization, and format versioning.
- [ ] 4.2 Run `openspec.cmd validate add-session-export`.
- [ ] 4.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 4.4 Run `.\gradlew.bat connectedDebugAndroidTest` when a device or emulator is available.
- [ ] 4.5 Manually verify save, share, cancellation, write failure, and missing optional sections.

## Suggested Commits

- `文档：新增导出格式规格`
- `功能：定义会话导出格式`
- `功能：生成会话导出内容`
- `功能：新增会话导出入口`
- `修复：完善导出失败处理`
- `测试：补充会话导出用例`
