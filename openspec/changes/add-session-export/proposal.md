## Why

Users need to take transcripts and recaps out of the app for notes, sharing, and archiving. Export should provide simple portable formats before adding heavier document formats.

## What Changes

- Add session export for Markdown, TXT, and JSON.
- Include transcript, summary, timeline chapters, generated tags, and favorited highlights when present.
- Use Android system save/share flows.
- Handle empty content, filename sanitization, write failures, and user cancellation.
- Do not add PDF, Word, cloud sync, or publishing in this change.

## Capabilities

### New Capabilities

- `session-export`: Export format contract, rendering, file naming, save/share behavior, error handling, and privacy rules.

### Modified Capabilities

- None.

## Impact

- Affects export rendering utilities, detail UI actions, Android document/share intents, unit tests, and device verification.
