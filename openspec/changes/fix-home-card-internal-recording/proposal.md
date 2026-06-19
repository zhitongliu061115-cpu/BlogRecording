# Fix Home Card Internal Recording

## Background
Home podcast cards currently start microphone recording, while system-audio recording lives behind a separate home action. This makes card-level recording inconsistent with the intended internal-audio workflow and leaves users without a quick transcript preview on each card.

## Goals
- Make each podcast card start and resume system-audio recording through MediaProjection.
- Show only the most recent podcast cards on Home so older or less useful cards move to History.
- Add a small scrollable transcript preview to each podcast card.
- Keep audio, PCM, real transcript data, logs, and secrets local and out of commits.

## Scope
- Update Home card recording actions, state mapping, and card UI.
- Extend recording orchestration so internal-audio segments can start, resume, pause, and finish from cards.
- Add focused tests for card ordering, transcript previews, and internal-audio controller behavior.
- Refresh outdated UI test expectations affected by current Home text.

## Out of Scope
- Cloud ASR or non-local transcription.
- Deleting historical podcast data.
- Changing DeepSeek summary payloads beyond existing transcript-only behavior.

## Acceptance
- Card Start and Resume launch system-audio permission flow and create internal-audio recording segments.
- Home shows the latest 5 podcast cards by updated time; History retains all records.
- Cards show recent non-empty transcript snippets in a bounded scroll area.
- Unit tests, OpenSpec validation, debug build, and connected Android tests are run or documented with blockers.
