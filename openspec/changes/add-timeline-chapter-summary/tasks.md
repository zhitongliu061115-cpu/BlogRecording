## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Context

- [x] 2.1 Define timeline chapter fields for start time, end time, title, key points, and transcript source range.
- [x] 2.2 Build timestamped transcript context from session transcript segments.
- [x] 2.3 Add tests for ordering, missing timestamps, reversed ranges, and overlapping ranges.

## 3. Generation And Parsing

- [x] 3.1 Update prompt construction to request timeline chapters from timestamped context.
- [x] 3.2 Parse timeline chapters and validate ranges against transcript segment bounds.
- [x] 3.3 Fall back to untimed sections when usable timestamps are unavailable.

## 4. UI

- [x] 4.1 Display chapter title, approximate time range, and key points on the detail screen.
- [x] 4.2 Keep transcript and existing structured summary sections available when no chapters exist.

## 5. Tests And Harness

- [x] 5.1 Add unit tests for timeline context, parser validation, fallback, and UI state mapping.
- [ ] 5.2 Run `openspec.cmd validate add-timeline-chapter-summary`.
- [ ] 5.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 5.4 Manually verify long content, multi-segment recordings, missing timestamps, and malformed timeline response.

## Suggested Commits

- `文档：新增时间线总结规格`
- `功能：新增时间线章节模型`
- `功能：生成时间线总结上下文`
- `功能：生成章节时间线总结`
- `功能：展示章节时间线`
- `测试：补充时间线总结用例`
