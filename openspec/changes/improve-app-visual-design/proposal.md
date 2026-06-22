## Why

The current Compose UI exposes the product's core features, but it still reads like a framework scaffold: dynamic system colors dilute the product identity, text-heavy controls compete for attention, repeated Home actions create ambiguity, and content-heavy screens lack a clear reading hierarchy. A focused visual redesign will make recording, reviewing, and asking questions feel calmer and more trustworthy without changing the local-first processing model.

## What Changes

- Introduce a stable product color system, typography scale, shapes, spacing, and reusable screen components for light and dark themes.
- Replace text-only navigation and common commands with familiar Material icons and accessible labels.
- Redesign Home around one prominent capture action area, a concise session library, clear processing state, and quieter model/privacy status.
- Redesign AI, Mine, History, Settings, and Detail screens with consistent app bars, grouped content, empty states, status treatments, and action hierarchy.
- Improve compact-screen behavior so actions wrap or collapse without clipping, overlap, or horizontal overflow.
- Preserve existing user-visible capabilities, callback contracts, privacy boundaries, and test tags unless a specification explicitly updates them.

Non-goals:

- Do not change recording, import, transcription, diarization, summary, QA, export, persistence, permission, or security behavior.
- Do not introduce a backend, account system, analytics SDK, remote images, or new runtime network dependency.
- Do not redesign the launcher icon or create marketing/landing-page content.

## Capabilities

### New Capabilities

- `app-visual-system`: Stable colors, typography, shapes, icon usage, spacing, shared page chrome, and accessibility behavior across the app.
- `content-first-workspace-ui`: User-visible layout and hierarchy for Home, AI, Detail, History, Mine, and Settings workflows.

### Modified Capabilities

- None. `openspec/specs/` currently has no stable baseline specs; this change adds new visual and workspace UI capabilities without altering existing domain behavior.

## Impact

- Affected code:
  - `app/src/main/java/com/example/blogrecording/ui/theme/`
  - `app/src/main/java/com/example/blogrecording/ui/components/`
  - `app/src/main/java/com/example/blogrecording/ui/HomeScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/AiChatScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/DetailScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/HistoryScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/MineScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/SettingsScreen.kt`
  - `app/src/main/java/com/example/blogrecording/ui/PodcastRecapApp.kt`
  - Focused Compose tests and UI contract tests
- No expected data migration, API change, permission change, or model change. The Compose Material icon artifact may be added for familiar command and navigation icons.
- Existing unit tests, instrumentation test APK build, Android lint, Debug build, and simulator visual QA remain required.
