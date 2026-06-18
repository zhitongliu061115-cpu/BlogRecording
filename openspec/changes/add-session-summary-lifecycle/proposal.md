## Why

Segmented podcast sessions can contain multiple recording segments, so summary generation must operate on the full podcast session transcript instead of a single recording run. Users also need durable summary status, retry behavior, and clear errors when a summary cannot start.

## What Changes

- Aggregate transcript segments for a podcast session in recording order before summary.
- Add session-level summary eligibility rules.
- Persist summary lifecycle state: not ready, ready, summarizing, summarized, and failed.
- Allow retry after failed summary without deleting the previous successful summary.
- Handle missing DeepSeek API key, network/API failures, long transcript behavior, and partial transcripts.
- Preserve privacy boundaries: do not log API keys, raw audio, PCM, full local paths, or full transcript text.

Non-goals:

- Do not change Android Keystore + AES/GCM API key storage.
- Do not add another LLM provider.
- Do not upload raw audio, PCM, or speaker embeddings.
- Do not build a prompt template marketplace.
- Do not change ASR, VAD, diarization, or bundled model verification.

## Capabilities

### New Capabilities

- `summary`: Session-level summary eligibility, start, retry, persistence, error handling, and privacy requirements.
- `transcription`: Session transcript aggregation and partial transcript behavior for multi-segment podcast sessions.

### Modified Capabilities

- None. `openspec/specs/` currently has no stable baseline specs; this change adds delta specs under the change directory.

## Impact

- Affected code areas:
  - `app/src/main/java/com/example/blogrecording/summary/`
  - `app/src/main/java/com/example/blogrecording/data/Models.kt`
  - `app/src/main/java/com/example/blogrecording/data/Repository.kt`
  - `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt`
  - Focused unit tests under `app/src/test/java/com/example/blogrecording/summary/` and `app/src/test/java/com/example/blogrecording/data/`
- Existing DeepSeek client, encrypted API key storage, local ASR, recording, and Home cards must keep working.
