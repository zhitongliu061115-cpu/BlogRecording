## Why

Users need the Home screen to manage multiple podcast sessions as independent, resumable recording units instead of treating the app as one global recording flow. This change makes the segmented recording domain visible and controllable from Home without changing the underlying recording, ASR, or summary pipelines.

## What Changes

- Replace the single global Home recording panel with a list of podcast session cards.
- Add a Home empty state with an entry to create a new podcast session.
- Show each podcast session's title, status, recording duration, segment count, transcription state, and summary state.
- Add card actions for create, rename, start recording, pause, resume, finish, and start summary.
- Disable invalid actions based on ViewModel-derived UI state.
- Highlight the one currently recording card.
- Keep all Home Composables driven by ViewModel state and lambda events.

Non-goals:

- Do not implement or alter the low-level recording service logic.
- Do not implement or alter DeepSeek summary generation.
- Do not change the ASR, VAD, diarization, model verification, Keystore, or DataStore persistence contracts.

## Capabilities

### New Capabilities

- `home-ui`: Home screen podcast card list, session creation entry, card actions, invalid action disabling, rename dialog, and empty state.

### Modified Capabilities

- None. `openspec/specs/` currently has no stable baseline specs; this change adds a delta spec under the change directory.

## Impact

- Affected code areas:
  - `app/src/main/java/com/example/blogrecording/ui/HomeScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt`
  - `app/src/main/java/com/example/blogrecording/ui/state/AppUiState.kt`
  - New or updated Home UI state/event helpers under `app/src/main/java/com/example/blogrecording/ui/`
  - Focused unit tests under `app/src/test/java/com/example/blogrecording/ui/`
- UI-only behavior change on Home; existing microphone recording, system recording, local ASR, DeepSeek summary, encrypted API key storage, and bundled model validation must keep working.
