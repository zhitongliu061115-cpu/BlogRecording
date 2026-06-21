## Why

Podcast sessions become harder to scan as history grows. Automatically generated keywords and tags give users fast topical cues on the home card and detail screen without requiring manual tagging.

## What Changes

- Generate keywords and tags from the session transcript and structured summary.
- Persist normalized tags with ordering, source, and generation status.
- Display tags on the home card and detail screen.
- Handle empty transcript, missing API key, duplicate tags, overly long tags, and retry failures.
- Do not add global search or tag-management screens in this change.

## Capabilities

### New Capabilities

- `keyword-tag-generation`: Automatic keyword/tag generation, normalization, persistence, failure handling, and display state.
- `home-ui`: Requirements for home cards to show generated tags compactly.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects summary-derived metadata, prompt/response parsing, repository compatibility, home/detail UI state, unit tests, and manual DeepSeek verification.
