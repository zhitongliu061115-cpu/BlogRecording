## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Compatibility

- [x] 2.1 Define structured summary fields for overview, key points, action items, open questions, and quote candidates.
- [x] 2.2 Persist structured summary with backward compatibility for legacy plain-text summaries.
- [x] 2.3 Add compatibility tests for old summaries, new structured summaries, and failed retry preservation.

## 3. Generation And Parsing

- [x] 3.1 Update summary prompt construction to request stable structured output.
- [x] 3.2 Implement structured response parsing with plain-text fallback.
- [x] 3.3 Ensure summary logs and errors do not expose API keys, raw transcript text, or provider payloads.

## 4. UI

- [x] 4.1 Display structured summary sections on the detail screen.
- [x] 4.2 Preserve legacy summary display when structured data is absent.

## 5. Tests And Harness

- [x] 5.1 Add unit tests for prompt output, parser success, parser fallback, old data compatibility, and UI state mapping.
- [ ] 5.2 Run `openspec.cmd validate add-structured-session-summary`.
- [ ] 5.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 5.4 Manually verify configured API key, missing API key, empty transcript, malformed model response, and retry failure.

## Suggested Commits

- `文档：新增结构化总结规格`
- `功能：新增结构化总结模型`
- `功能：生成结构化总结`
- `功能：展示结构化总结`
- `测试：补充结构化总结用例`
