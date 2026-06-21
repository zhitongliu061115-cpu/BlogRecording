## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Generation

- [ ] 2.1 Define highlight model with text, source time range, transcript segment IDs, generated/favorite state, and timestamps.
- [ ] 2.2 Generate highlight candidates from structured summary quote candidates or transcript fallback.
- [ ] 2.3 Deduplicate highlights by normalized text and source range.

## 3. User Actions And UI

- [ ] 3.1 Support favorite and unfavorite actions with persistent state.
- [ ] 3.2 Preserve user-favorited highlights when summary/highlight generation is retried.
- [ ] 3.3 Display highlights and favorite state on the detail screen.

## 4. Tests And Harness

- [ ] 4.1 Add unit tests for source mapping, favorite toggling, deduplication, regeneration preservation, and compatibility.
- [ ] 4.2 Run `openspec.cmd validate add-session-highlights`.
- [ ] 4.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 4.4 Manually verify favorite, unfavorite, regeneration, missing source time, and duplicate candidate behavior.

## Suggested Commits

- `文档：新增高光收藏规格`
- `功能：新增高光收藏模型`
- `功能：生成高光金句候选`
- `功能：支持高光收藏操作`
- `功能：展示高光收藏`
- `测试：补充高光收藏用例`
