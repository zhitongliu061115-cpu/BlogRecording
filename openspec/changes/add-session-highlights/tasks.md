## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Generation

- [x] 2.1 Define highlight model with text, source time range, transcript segment IDs, generated/favorite state, and timestamps.
- [x] 2.2 Generate highlight candidates from structured summary quote candidates or transcript fallback.
- [x] 2.3 Deduplicate highlights by normalized text and source range.

## 3. User Actions And UI

- [x] 3.1 Support favorite and unfavorite actions with persistent state.
- [x] 3.2 Preserve user-favorited highlights when summary/highlight generation is retried.
- [x] 3.3 Display highlights and favorite state on the detail screen.

## 4. Tests And Harness

- [x] 4.1 Add unit tests for source mapping, favorite toggling, deduplication, regeneration preservation, and compatibility.
- [x] 4.2 Run `openspec.cmd validate add-session-highlights`.
- [x] 4.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 4.4 Manually verify favorite, unfavorite, regeneration, missing source time, and duplicate candidate behavior.

## Suggested Commits

- `文档：新增高光收藏规格`
- `功能：新增高光收藏模型`
- `功能：生成高光金句候选`
- `功能：支持高光收藏操作`
- `功能：展示高光收藏`
- `测试：补充高光收藏用例`
