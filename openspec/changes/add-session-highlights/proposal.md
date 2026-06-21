## Why

Users often want to keep memorable quotes, insights, or high-value moments from a single episode. Highlights turn generated recap content into a personal collection without requiring export or social sharing first.

## What Changes

- Add highlight and quote candidate data with text, source time range, source transcript segment IDs, and favorite state.
- Generate highlight candidates from structured summary or transcript.
- Allow users to favorite and unfavorite highlights.
- Display highlights on the detail screen.
- Preserve user-favorited highlights when summary regeneration changes candidates.

## Capabilities

### New Capabilities

- `session-highlights`: Highlight candidate generation, source mapping, favorite persistence, display, and regeneration behavior.

### Modified Capabilities

- None.

## Impact

- Affects recap metadata models, repository compatibility, detail UI, unit tests, and manual verification.
- Does not add public sharing, social publishing, or cross-session highlight search.
