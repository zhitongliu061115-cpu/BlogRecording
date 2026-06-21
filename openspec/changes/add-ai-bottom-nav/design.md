## Overview

Add AI and Mine as top-level destinations while preserving existing Home, Settings, History, and Detail flows. The AI destination is a session-scoped assistant chat surface that reuses the same DeepSeek single-session QA pipeline already used by the podcast detail page.

## Current Context

- `AppScreen` currently has `HOME`, `SETTINGS`, `HISTORY`, and `DETAIL`.
- `PodcastRecapApp` switches directly on `state.currentScreen`.
- `HomeScreen` already renders podcast cards from `HomeUiState.cards`.
- Detail already contains single-session QA logic, but there is no top-level AI tab.

## Design

### Navigation

Extend `AppScreen` with:

- `AI`: top-level assistant page.
- `MINE`: top-level profile/settings hub.

Render a shared Material bottom bar only for top-level screens: `HOME`, `AI`, and `MINE`. Keep Settings, History, and Detail outside the bottom bar so existing back behavior remains simple.

Mine acts as a lightweight hub with entries to Settings and History, preserving those routes without exposing them as bottom tabs.

### AI UI State

Add AI-specific immutable state under `ui/state`:

- `AiChatUiState`: selected session id, chooser visibility, card list, messages, and input text.
- `AiPodcastCardUiState`: session id, title, metadata labels, tags, and transcript preview.
- `AiChatMessageUiState`: id, text, sender, timestamp label, status, retry target id, and optional error label.

Derive AI podcast cards from existing `HomeUiState.cards` to avoid duplicating data mapping. The first AI entry starts with `isChoosingPodcast = true` when no selected session exists. After selection, map the selected session's persisted `SessionQaHistory` into chat bubbles so the AI page and detail page show the same conversation history.

### DeepSeek QA Reuse

Do not create a separate chat API. The AI page calls existing `SessionQaUseCase.ask(sessionId, question, settings)` through `AppViewModel`, exactly like detail-screen QA. This preserves:

- API key checks from `ApiKeyStore`.
- Context construction from summary, timeline, highlights, tags, and transcript excerpts.
- Error statuses for missing API key, empty content, failed request, and retryable failures.
- Persisted per-session `qaHistory`.

When a selected session's QA history changes, refresh `AiChatUiState.messages` from the same persisted history. Sending from the AI page appends to that history; opening the same session in detail shows the same questions and answers.

### AI Screen

`AiChatScreen` is stateless:

- Top app bar title: AI 助手.
- Right action: 新对话.
- If choosing, show a horizontal card row (`LazyRow`) of podcast choices and an empty-state message if no podcast exists.
- If a session is selected, show a WeChat-like message list with user messages right-aligned and assistant messages left-aligned.
- Input row allows typing and sending. Sending delegates to the existing DeepSeek QA use case, clears the draft, and shows the persisted answering/answered/failed state.
- Failed messages expose retry from the AI page using the same retry path as detail QA.

### Scope Boundaries

- Do not add a new network request path beyond the existing DeepSeek QA client.
- Do not create separate top-level AI chat persistence; reuse session `qaHistory`.
- Do not fork or duplicate the existing detail QA use case.
- Do not change recording, import, summary, API key, or model loading behavior.

## Risks

- Top-level route changes can hide Settings/History. Mitigation: expose them in Mine and keep Home header actions as-is.
- AI page and detail page could drift if they keep separate message state. Mitigation: map both from persisted session `qaHistory` and update the selected session after each QA result.
- Compose surface changes can regress navigation. Mitigation: add pure policy/state tests and run unit harness.
