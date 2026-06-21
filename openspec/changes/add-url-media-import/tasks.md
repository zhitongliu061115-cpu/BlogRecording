## 1. Specification

- [ ] 1.1 Validate the OpenSpec change and keep proposal, design, specs, tasks, and harness aligned.

## 2. URL Import Model And UI

- [ ] 2.1 Add URL import state, validation result, and sanitized source metadata.
- [ ] 2.2 Add URL input UI with Xiaoyuzhou episode URL examples and clear validation messages.

## 3. URL Resolution And Download

- [ ] 3.1 Implement Xiaoyuzhou episode URL detection and public media resolution.
- [ ] 3.2 Implement direct media URL and RSS enclosure fallback resolution.
- [ ] 3.3 Download resolved media to app-private cache with timeout, redirect, and size limits.
- [ ] 3.4 Reuse the local imported-media processing flow after download.

## 4. Failure Handling And Privacy

- [ ] 4.1 Handle invalid URL, unsupported page, private/blocked episode, timeout, 401, 429, non-2xx, empty response, and oversized download.
- [ ] 4.2 Ensure logs and user-visible errors do not include API keys, cookies, raw audio, private cache paths, or full transcript text.

## 5. Tests And Harness

- [ ] 5.1 Add unit tests for Xiaoyuzhou URL parsing, resolver priority, fixture parsing, network response classification, and privacy-safe logging.
- [ ] 5.2 Run `openspec.cmd validate add-url-media-import`.
- [ ] 5.3 Run `git diff --check`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 5.4 Manually verify a Xiaoyuzhou episode URL, invalid URL, unsupported URL, direct media URL, RSS enclosure,断网, slow network, and non-downloadable resource.

## Suggested Commits

- `文档：新增 URL 导入规格`
- `功能：新增 URL 导入入口`
- `功能：支持小宇宙单期链接解析`
- `功能：支持 URL 媒体解析下载`
- `功能：复用导入媒体转写流程`
- `修复：完善 URL 导入失败处理`
- `测试：补充 URL 导入用例`
