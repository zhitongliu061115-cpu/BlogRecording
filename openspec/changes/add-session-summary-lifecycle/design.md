## Context

Current DeepSeek call chain evidence:

- `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt` reads the DeepSeek API key through `ApiKeyStore.readApiKey()`, checks `session.transcript`, and calls `SummaryRepository.generateSummary(apiKey, session.transcript, settings)`.
- `app/src/main/java/com/example/blogrecording/summary/SummaryRepository.kt` validates `apiKey` and `transcript`, chunks the transcript with `TranscriptChunker`, builds prompts with `SummaryPromptBuilder`, calls `DeepSeekSummaryClient.summarize()` for each chunk, and performs a final merge request when there is more than one chunk.
- `app/src/main/java/com/example/blogrecording/summary/DeepSeekSummaryClient.kt` sends requests to `https://api.deepseek.com/chat/completions`, uses `Authorization: Bearer <apiKey>`, parses `choices[].message.content`, and logs only bounded failure metadata such as HTTP code, response field keys, class names, and response length.
- `app/src/main/java/com/example/blogrecording/security/ApiKeyStore.kt` stores the API key with Android Keystore + AES/GCM and returns `DeepSeekApiKeyMissing` when no encrypted payload exists.

Current transcript data structure evidence:

- `app/src/main/java/com/example/blogrecording/data/Models.kt` defines `PodcastSession.transcript`, `PodcastSession.summary`, `RecordingSegment.index`, `RecordingSegment.transcriptSegmentIds`, and `TranscriptSegmentEntity.recordingSegmentId`.
- `app/src/main/java/com/example/blogrecording/data/Repository.kt` stores transcript segments under the session id, sorts transcript segments by `startMs`, rebuilds session transcript text, and attaches transcript ids to matching recording segments when `recordingSegmentId` is present.
- `add-segmented-recording-resume` associates ASR output with active recording segment ids.
- `add-home-podcast-cards` exposes summary button eligibility from Home UI state but does not implement durable session summary lifecycle.

## Goals / Non-Goals

**Goals:**

- Build a session aggregate transcript from all available transcript segments for the target podcast session.
- Persist summary lifecycle state on `PodcastSession.summary`.
- Allow retries after summary failure.
- Preserve any previous successful summary while a retry runs or fails.
- Handle missing API key, network errors, DeepSeek API errors, transcript-too-long policy, and partial transcripts.
- Keep privacy constraints explicit and testable.

**Non-Goals:**

- Do not change Keystore encryption or API key storage.
- Do not change the LLM provider or endpoint.
- Do not upload audio, PCM, speaker embeddings, or raw local audio files.
- Do not change ASR, VAD, diarization, or model verification.
- Do not build new Home card layout behavior in this change.

## Decisions

### Transcript aggregation algorithm

For a `PodcastSessionDetail`:

1. Read `recordingSegments` for the session and sort by `RecordingSegment.index`.
2. Read `transcriptSegments` for the session.
3. For each recording segment, include transcript segments whose `recordingSegmentId` equals that segment id, sorted by `startMs`.
4. Then include legacy transcript segments with no `recordingSegmentId`, sorted by `startMs`, when they belong to the same session and were not already included.
5. Drop blank transcript text.
6. Format each included transcript with timestamp and speaker label using the existing `toTranscriptText()` format.
7. Join entries with blank lines.

This keeps multi-segment sessions ordered while preserving legacy transcript data.

### Summary prompt input format

The prompt input remains a text transcript. For MVP, use the aggregate transcript text produced above and pass it to the existing `SummaryRepository.generateSummary()` pipeline. The prompt builder may receive session metadata later, but this change should not require new provider-specific prompt schema.

### Summary status model

Use existing `SummaryStatus` values:

- `NotReady`: no aggregate transcript is available or the session is recording.
- `Ready`: aggregate transcript exists and no active recording blocks final summary.
- `Summarizing`: a DeepSeek request is in progress.
- `Summarized`: latest successful summary is persisted.
- `Failed`: latest attempt failed with a recoverable error.

`PodcastSessionStatus` should move consistently:

- Start: `READY_FOR_SUMMARY` or `SUMMARIZED` -> `SUMMARIZING`.
- Success: `SUMMARIZING` -> `SUMMARIZED`.
- Failure: `SUMMARIZING` -> `READY_FOR_SUMMARY` or `ERROR` only if the state machine requires it, with `SessionSummary.status = FAILED`.

### Retry and previous summary preservation

`SessionSummary` should retain successful summary text when a retry fails. If a retry is started, store retry state separately through status/error fields without clearing `text` until a new successful summary replaces it.

### Error handling

- API Key missing: return `DeepSeekApiKeyMissing`, do not call DeepSeek, persist summary status `FAILED` with bounded error message.
- Network error: persist `FAILED`, keep previous summary text if present, allow retry.
- DeepSeek error response: persist `FAILED`, store bounded error message from `DeepSeekSummaryClient`.
- Transcript too long: MVP continues using existing chunk/map-reduce strategy in `SummaryRepository`; if chunking cannot produce bounded prompts, return `TranscriptTooLong` and persist `FAILED`.

### Long text strategy

MVP uses the existing chunk summary plus final merge approach:

- `TranscriptChunker(maxCharsPerChunk = 12_000)` splits long transcript text.
- Each chunk is summarized independently.
- Multiple partial summaries are merged by `SummaryPromptBuilder.buildFinalPrompt()`.

Future work can add token-aware chunking, but this change should keep the current char-based strategy and add tests around it.

### Privacy and logging

- Never log API keys or authorization headers.
- Never log full transcript text.
- Never log raw audio, PCM bytes, or full local audio paths.
- DeepSeek failures may log HTTP status, JSON field names, class names, and bounded response length.
- User-visible errors should be bounded status messages, not raw provider payloads.

### Testing strategy

- Unit tests for transcript aggregation order across recording segments.
- Unit tests for partial transcript and failed-segment preservation.
- Unit tests for summary eligibility.
- Unit tests for start summary, success, failure, retry, and previous summary preservation with fake DeepSeek client/repository.
- Tests for missing API key ensuring no DeepSeek call is made.
- Regression tests for `SummaryRepository` chunk/map-reduce behavior.
- Log/privacy tests or code-level assertions that error paths do not include API keys or full transcript text.

## Risks / Trade-offs

- Char-based chunking may not map exactly to model token limits -> Keep current chunking for MVP and return bounded failure if provider rejects length.
- Existing `AppViewModel` has both legacy session and podcast session paths -> Add a summary use case or repository helper to avoid spreading lifecycle logic through UI code.
- Legacy transcript segments may lack `recordingSegmentId` -> Include them after segment-linked transcripts so old data remains summarizable.
- Retry state can overwrite summary text if modeled carelessly -> Tests must assert previous summary preservation.

## Migration Plan

1. Add transcript aggregation helper and tests.
2. Add summary eligibility policy and tests.
3. Add repository persistence helpers for summary status updates.
4. Add session summary use case with fake DeepSeek client tests.
5. Wire ViewModel/Home card summary action to the new use case.
6. Run OpenSpec validate, targeted tests, `testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug`.

Rollback:

- Existing legacy `RecordingSessionEntity.summary` remains readable.
- If session-level summary fails, existing transcript and prior summary text remain persisted.

## Open Questions

- Should a recording session support draft summary while still recording, or should final summary remain blocked until pause/finish only?
- Should transcript-too-long be a hard failure or should the app recursively summarize partial summaries until within prompt limits?
- Should the summary detail UI show both previous successful summary and latest failed retry error?
