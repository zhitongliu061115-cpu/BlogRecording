## Context

The app is a single Android Compose client with local recording, ASR, VAD, diarization, DataStore persistence, and optional direct DeepSeek requests. Its domain behavior is mature enough to expose a broad workflow, but the UI still uses the default Compose theme, Android dynamic colors, text-only bottom navigation, repeated Home actions, and large groups of equally weighted buttons.

The visual reference set includes podcast knowledge tools such as Snipd and Podwise and transcription workspaces such as Notta and Otter. The useful shared patterns are content-first episode cards, explicit capture state, readable summaries, progressive disclosure, and a conversation surface grounded in selected source material. This design adapts those patterns to a private, local-first Android tool rather than reproducing cloud dashboard or subscription-product conventions.

Constraints:

- Preserve existing callback contracts, state models, privacy copy, test tags, and domain behavior where practical.
- Remain usable on compact Android phone viewports.
- Avoid remote visual assets and runtime network dependencies.
- Use Material 3 and Compose-native components.
- Keep cards at 8 dp radius or less and avoid cards nested inside cards.

## Goals / Non-Goals

**Goals:**

- Establish a recognizable and stable product identity.
- Make the next action obvious on Home and session cards.
- Improve reading order for summaries, highlights, transcript, and QA.
- Use icons, grouped controls, semantic status, and intentional empty states.
- Keep light and dark themes coherent.
- Create shared UI primitives that reduce page-by-page drift.

**Non-Goals:**

- Change session state machines, recording source behavior, import behavior, persistence, models, DeepSeek prompts, or exports.
- Introduce animation-heavy branding, illustrations, onboarding, accounts, or cloud sync.
- Rebuild navigation architecture or add new destinations.

## Decisions

### Stable theme instead of dynamic color

Disable dynamic color by default. Use a neutral graphite and soft gray foundation, coral-red for recording and primary commands, teal for local/safe/ready states, amber for warnings, and semantic Material error colors.

Light theme roles:

- background: `#F7F7F4`
- surface: `#FFFFFF`
- onSurface: `#1B1C1E`
- primary: `#C94736`
- secondary: `#17756D`
- tertiary: `#A06A16`

Dark theme uses near-black neutral surfaces with softened coral and teal accents. This avoids a one-note palette while keeping recording state recognizable.

Alternative considered: retain Android dynamic color. Rejected because screenshots and status semantics vary by wallpaper, weakening product identity and occasionally producing low-emphasis recording controls.

### Shared page chrome and primitives

Add reusable components for:

- `PodcastTopBar`
- `SectionHeader`
- `StatusPill`
- `EmptyState`
- `SettingSection`
- icon action buttons with tooltips/content descriptions

Use full-width page layouts with constrained inner padding. Cards are reserved for session items and genuinely grouped content. Section grouping in Settings and Detail uses surface bands, dividers, or simple columns rather than nested cards.

Alternative considered: redesign each page independently. Rejected because it would preserve visual drift and duplicate accessibility decisions.

### Icon strategy

Add the Compose Material Icons Extended artifact and use Material icons for bottom navigation, back, settings, history, edit, add, microphone, screen capture, import, link, play/pause, stop, summarize, copy, export, share, delete, send, retry, and status.

Icon-only buttons use `contentDescription`; unfamiliar icons also receive tooltips. Commands with material consequences keep icon plus text.

### Home hierarchy

Home has:

1. Product/page header with compact History and Settings icon commands.
2. A single capture/import source strip.
3. Session library or an intentional empty state.
4. Compact local-processing readiness and privacy band.

When sessions exist, there is no second duplicate create/import button group. Session cards expose one primary valid action and place secondary actions in a compact row or overflow treatment.

### Detail reading order

Detail order becomes:

1. App bar and session metadata.
2. Processing/error state.
3. AI summary and timeline.
4. Highlights and tags.
5. Transcript.
6. QA.
7. Export/copy utility actions.
8. Speaker note and separated delete action.

This order treats the screen as a review workspace rather than a debug dump.

### Settings controls

Use switches for binary values, dropdowns for enum options, sliders for bounded counts, and numeric fields for expert tuning. Group fields into API, summary, local processing, models, and privacy sections. Destructive API-key deletion uses error styling and remains secondary to save.

### Verification strategy

- Preserve existing behavior-focused tests.
- Add source-level or Compose tests for stable theme, navigation icons, grouped settings, and key empty states where useful.
- Build instrumentation APK and run connected tests where the existing emulator is available.
- Capture screenshots for Home, AI, Mine, Settings, History, and a populated session path when local state permits.
- Check compact portrait layout for clipping and overlap.

## Risks / Trade-offs

- [Large cross-screen patch increases regression risk] -> Introduce shared primitives first, migrate one screen at a time, and compile/test between groups.
- [Material Icons Extended increases build artifact size] -> Accept the compile-time dependency because the APK is already dominated by bundled ONNX assets; use only official Compose ecosystem APIs.
- [Existing text-based UI tests may become brittle] -> Preserve important labels and test tags, and prefer semantic tags for new icon-only controls.
- [No seeded rich session data for screenshots] -> Verify empty and base states on the emulator, and use Compose previews/tests for populated component states.
- [Dark theme can lag behind light theme polish] -> Define both schemes together and run at least one dark-theme screenshot check.

## Migration Plan

1. Add theme tokens, icons dependency, and shared components.
2. Migrate bottom navigation and top-level page chrome.
3. Redesign Home and session cards.
4. Redesign AI and Mine.
5. Redesign History, Settings, and Detail.
6. Update focused UI tests and run OpenSpec validation.
7. Run unit tests, lint, Debug build, instrumentation APK build, connected tests, and simulator screenshot QA.

Rollback:

- Revert the UI/theme files and icon dependency. No stored data or domain migration is involved.

## Open Questions

- A future change may add user-selectable theme mode; this change follows system light/dark mode only.
- A future change may add waveform visualization; this change uses status and progress UI without inventing waveform data.
