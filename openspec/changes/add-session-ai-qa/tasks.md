## 1. Specification

- [x] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. Data And Context

- [x] 2.1 Define session QA message model with question, answer, timestamps, status, model name, and sanitized error.
- [x] 2.2 Persist QA history under one podcast session with backward-compatible defaults.
- [x] 2.3 Build deterministic single-session context from summary, timeline, highlights, tags, and transcript excerpts.
- [x] 2.4 Add context truncation and tests that prove other sessions are excluded.

## 3. DeepSeek QA Flow

- [x] 3.1 Add prompt builder that instructs answers to stay within episode content.
- [x] 3.2 Send QA requests only when API key and session content are available.
- [x] 3.3 Handle missing API key, missing content, timeout, 401, 429, non-2xx, empty response, and retry.
- [x] 3.4 Ensure logs and errors do not include API keys, raw prompt context, full question/answer bodies, or private paths.

## 4. UI

- [x] 4.1 Add detail-screen QA entry, question input, send action, and QA history.
- [x] 4.2 Show loading, blocked, failed, retryable, and answered states.

## 5. Tests And Harness

- [x] 5.1 Add unit tests for context construction, truncation, cross-session exclusion, blocked states, response handling, retry, and privacy-safe logging.
- [ ] 5.2 Run `openspec.cmd validate add-session-ai-qa`.
- [ ] 5.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 5.4 Manually verify configured API key, missing API key, no transcript, long transcript, network error, retry, and follow-up history.

## Suggested Commits

- `文档：新增单期 AI 问答规格`
- `功能：新增单期问答模型`
- `功能：构建单期问答上下文`
- `功能：接入单期 AI 问答`
- `功能：展示单期问答界面`
- `测试：补充单期问答用例`
