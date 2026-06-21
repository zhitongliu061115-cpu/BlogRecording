## Why

After a transcript and recap exist, users often want to ask follow-up questions about that specific episode. Single-session AI QA lets them query one episode without building cross-episode search first.

## What Changes

- Add a detail-screen QA entry for one podcast session.
- Build QA context only from the current session's transcript, structured summary, timeline chapters, tags, and highlights.
- Send questions to DeepSeek when an API key and session content are available.
- Persist session-level question/answer history.
- Handle missing API key, missing content, long context truncation, network errors, and retry.
- Do not add cross-session retrieval, web search, streaming answers, or shared bot memory.

## Capabilities

### New Capabilities

- `session-ai-qa`: Single-session question answering, context construction, chat history, UI states, and failure handling.
- `api-and-logging-privacy-boundary`: Privacy requirements for QA prompts, answers, logs, and errors.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects detail UI, QA context selection, DeepSeek request handling, QA persistence, unit tests, and manual API/network verification.
