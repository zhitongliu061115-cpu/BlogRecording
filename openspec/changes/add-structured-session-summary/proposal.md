## Why

The current summary path stores a single text blob, which is hard to render consistently, export, reuse for tags/highlights, or use as grounding for later QA. Structured summary turns each episode recap into stable sections that downstream features can consume.

## What Changes

- Add a structured summary model for overview, key points, action items, open questions, and quote candidates.
- Update DeepSeek prompt requirements to request a stable structured response.
- Parse structured responses with a safe fallback to legacy plain-text summary.
- Display structured sections on the detail screen while preserving old summary text compatibility.
- Keep API keys, raw transcript text, and provider payloads out of logs.

## Capabilities

### New Capabilities

- `structured-summary`: Structured summary schema, parsing, fallback, persistence, and display contract.
- `summary`: Requirements for session summary generation to produce and preserve structured summary data.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects summary data models, prompt building, response parsing, repository compatibility, detail UI, unit tests, and manual DeepSeek verification.
- No new external provider is introduced.
