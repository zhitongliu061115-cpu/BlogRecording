## Why

Long episodes are difficult to review from a flat transcript or plain summary. Chapter and timeline summaries let users scan what happened when and jump mentally to the parts they care about.

## What Changes

- Add timeline chapter data with start time, end time, title, key points, and source transcript range.
- Generate chapters from transcript segments and structured summary context.
- Display timeline chapters on the detail screen.
- Handle missing or unreliable timestamps with a clear fallback.
- Keep this change independent from playback seeking and speaker diarization quality improvements.

## Capabilities

### New Capabilities

- `timeline-summary`: Chapter/timeline summary generation, validation, persistence, and display.
- `structured-summary`: Requirements for structured summaries to expose timeline-compatible context.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects transcript segment aggregation, summary prompt/context building, response parsing, summary persistence, detail UI, and unit/manual tests.
