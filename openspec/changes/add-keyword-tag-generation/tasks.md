## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Generation

- [x] 2.1 Define keyword/tag model with display text, normalized key, order, source, timestamp, and status.
- [x] 2.2 Implement generation from structured summary when available and transcript fallback when absent.
- [x] 2.3 Normalize duplicates, trim whitespace, cap tag length, and cap tag count.

## 3. UI And Failure States

- [x] 3.1 Display a compact tag subset on home cards.
- [x] 3.2 Display the full generated tag list on the detail screen.
- [x] 3.3 Handle missing API key, empty transcript, repeated tags, long tags, and failed retry.

## 4. Tests And Harness

- [x] 4.1 Add unit tests for tag parsing, normalization, duplicate removal, compatibility, blocked states, and UI state mapping.
- [x] 4.2 Run `openspec.cmd validate add-keyword-tag-generation`.
- [x] 4.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 4.4 Manually verify Chinese content, English content, short content, duplicate tags, and failed retry.

## Suggested Commits

- `文档：新增关键词标签规格`
- `功能：新增标签数据模型`
- `功能：自动生成内容标签`
- `功能：展示自动标签`
- `修复：完善标签生成边界`
- `测试：补充关键词标签用例`
