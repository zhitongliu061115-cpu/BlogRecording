## Overview

Add AI and Mine as top-level destinations while preserving existing Home, Settings, History, and Detail flows. The AI destination is a session-scoped assistant chat mock surface backed by local UI state first, so this change can ship quickly without adding new storage or network behavior.

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
- `AiChatMessageUiState`: id, text, sender, timestamp label, and status.

Derive AI podcast cards from existing `HomeUiState.cards` to avoid duplicating data mapping. The first AI entry starts with `isChoosingPodcast = true` when no selected session exists.

### AI Screen

`AiChatScreen` is stateless:

- Top app bar title: AI 助手.
- Right action: 新对话.
- If choosing, show a horizontal card row (`LazyRow`) of podcast choices and an empty-state message if no podcast exists.
- If a session is selected, show a WeChat-like message list with user messages right-aligned and assistant messages left-aligned.
- Input row allows typing and sending. For this UI-first change, sending appends the user message and a local assistant placeholder that tells users this chat is scoped to the selected podcast.

### Scope Boundaries

- Do not add a new network request path.
- Do not persist top-level AI chat history.
- Do not change existing detail QA use case.
- Do not change recording, import, summary, API key, or model loading behavior.

## Risks

- Top-level route changes can hide Settings/History. Mitigation: expose them in Mine and keep Home header actions as-is.
- AI chat may be mistaken for full DeepSeek QA. Mitigation: local assistant placeholder copy is explicit that the conversation is scoped to the selected podcast.
- Compose surface changes can regress navigation. Mitigation: add pure policy/state tests and run unit harness.
