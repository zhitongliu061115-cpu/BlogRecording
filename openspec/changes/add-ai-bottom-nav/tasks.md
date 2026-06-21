## 1. Specification

- [x] 1.1 Validate proposal, design, specs, tasks, and harness for the AI bottom navigation change.

## 2. Navigation

- [x] 2.1 Extend app navigation state with AI and Mine top-level routes; verify with navigation policy tests.
- [x] 2.2 Add a shared bottom navigation bar with 首页, AI, 我的 and keep secondary routes outside the bottom bar; verify with compile.

## 3. AI Page

- [x] 3.1 Add AI chat UI state and mapper from existing Home podcast cards; verify first-entry chooser and new-conversation behavior with unit tests.
- [x] 3.2 Implement the AI chat screen with horizontal podcast cards, WeChat-like message bubbles, input row, and top-right new conversation action; verify with compile.
- [x] 3.3 Replace local placeholder AI replies with existing DeepSeek session QA calls; verify persisted QA messages are shown on AI and detail flows.
- [x] 3.4 Show answering, failed, missing API key, empty content, and retry states on the AI page using the same QA status semantics as detail.

## 4. Mine Page

- [x] 4.1 Add a Mine hub screen with Settings and History entries so secondary routes remain reachable; verify with compile.

## 5. Harness

- [x] 5.1 Run `openspec.cmd validate add-ai-bottom-nav`.
- [x] 5.2 Run `git diff --check`.
- [x] 5.3 Run targeted unit tests for UI navigation, AI chat state, and QA reuse.
- [x] 5.4 Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug` if time and environment allow.
- [ ] 5.5 Manually inspect the AI screen flow for first entry, card selection, new conversation, and bottom tab labels. Not run: no connected adb device or emulator.
