## Context

This change introduces the domain and persistence foundation for segmented podcast sessions. It does not connect UI cards, real pause/resume capture, or DeepSeek session summary behavior yet.

Current code evidence:

- `app/src/main/java/com/example/blogrecording/data/Models.kt` defines `RecordingSessionEntity`, `TranscriptSegmentEntity`, `SpeakerProfileEntity`, `RecordingStatus`, `AudioSourceType`, summary settings, and startup-interruption helpers.
- `app/src/main/java/com/example/blogrecording/data/Repository.kt` stores recording history in Preferences DataStore as JSON strings and exposes `sessions`, `createSession`, `saveSession`, `getSession`, `deleteSession`, `saveSegments`, `appendSegment`, `getSegments`, `saveSpeakerProfiles`, `getSpeakerProfiles`, `updateSummary`, and `markInterruptedSessions`.
- `app/src/main/java/com/example/blogrecording/data/RecordingPersistenceContract.kt` fixes the existing storage contract: DataStore name, session order key, session key family, transcript segment key family, speaker profile key family, and existing JSON field names.
- `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt` currently creates one recording session per recording run and treats `stopRecording()` as completion.
- `app/src/main/java/com/example/blogrecording/asr/TranscriptAssembler.kt` creates transcript segments keyed by session id.
- `app/src/main/java/com/example/blogrecording/summary/SummaryRepository.kt` summarizes one transcript string.

Current storage has no recording-segment metadata and no schema version. Existing transcript segments are ASR text segments, not recording-session segments.

## Goals / Non-Goals

**Goals:**

- Add durable podcast session and recording segment domain models.
- Preserve old recording history and existing transcript/speaker associations.
- Define a session status lifecycle that supports draft, recording, paused, processing, ready-for-summary, summarizing, summarized, and error.
- Add repository APIs that future recording, UI, and summary changes can depend on.
- Add recovery metadata for interrupted active sessions and incomplete segments.
- Cover model/state/persistence behavior with unit tests.

**Non-Goals:**

- Do not implement Home podcast cards.
- Do not implement AudioRecord or MediaProjection pause/resume behavior.
- Do not implement DeepSeek session-summary lifecycle.
- Do not change SenseVoice, sherpa-onnx, VAD, diarization, model installation, or `verifyBundledModels`.
- Do not remove old recording-history read paths.

## Decisions

### Target Data Model

Add domain entities in the data layer while keeping old entities readable.

`PodcastSession`

- `id: String`
- `title: String`
- `createdAt: Long`
- `updatedAt: Long`
- `sourceType: AudioSourceType?`
- `status: PodcastSessionStatus`
- `activeSegmentId: String?`
- `lastCompletedSegmentId: String?`
- `transcript: String`
- `summary: SessionSummary?`
- `summaryStyle: SummaryStyle`
- `summaryLanguage: SummaryLanguage`
- `summaryModelName: String`
- `asrModelName: String`
- `vadModelName: String`
- `diarizationModelName: String`
- `detectedSpeakerCount: Int`
- `recordingSegmentCount: Int`
- `transcriptSegmentCount: Int`
- `errorMessage: String?`
- `legacyRecordingSessionId: String?`

`RecordingSegment`

- `id: String`
- `sessionId: String`
- `index: Int`
- `sourceType: AudioSourceType`
- `status: RecordingSegmentStatus`
- `startedAt: Long`
- `endedAt: Long?`
- `durationMs: Long`
- `pcmFilePath: String?`
- `audioFilePath: String?`
- `sampleRate: Int?`
- `channelCount: Int?`
- `transcriptSegmentIds: List<String>`
- `errorMessage: String?`
- `createdAt: Long`
- `updatedAt: Long`

`SessionSummary`

- `text: String`
- `status: SummaryStatus`
- `modelName: String`
- `generatedAt: Long?`
- `updatedAt: Long`
- `errorMessage: String?`

`TranscriptSegment`

- Keep existing `TranscriptSegmentEntity` fields for compatibility.
- Future changes may add `recordingSegmentId: String?`, but this change can leave existing transcript storage intact unless tests require direct segment association.

Enums:

- `PodcastSessionStatus`: `DRAFT`, `RECORDING`, `PAUSED`, `PROCESSING`, `READY_FOR_SUMMARY`, `SUMMARIZING`, `SUMMARIZED`, `ERROR`
- `RecordingSegmentStatus`: `RECORDING`, `PAUSED`, `PROCESSING`, `COMPLETED`, `INTERRUPTED`, `ERROR`
- `SummaryStatus`: `NOT_READY`, `READY`, `SUMMARIZING`, `SUMMARIZED`, `FAILED`

### State Machine

| Action | From | To | Persistence effect |
| --- | --- | --- | --- |
| create | none | Draft | Persist session with no recording segments |
| start | Draft, Paused, ReadyForSummary, Error | Recording | Set `activeSegmentId` after appending a recording segment |
| pause | Recording | Paused | Mark active segment ended/paused and clear `activeSegmentId` |
| resume | Paused, ReadyForSummary, Error | Recording | Append a new recording segment and set `activeSegmentId` |
| finalize | Paused, Processing | ReadyForSummary | Preserve all segments and transcript state |
| startTranscription | Recording, Paused | Processing | Mark segment/session processing without losing existing transcript |
| markReadyForSummary | Processing, Paused | ReadyForSummary | Requires non-empty finalized transcript for final summary eligibility |
| summarize | ReadyForSummary | Summarizing | Store summary status in progress |
| summarize success | Summarizing | Summarized | Persist summary text and timestamp |
| fail | any | Error | Persist error while preserving segments and transcript |
| recover | Recording, Processing, Summarizing | Paused or Error | Clear active recording and preserve durable data |

This change implements only the domain/persistence pieces needed for the table. Real recording actions are wired in later changes.

### Persistence Design

Keep Preferences DataStore for this change to preserve the current app identity and storage contract.

- Existing DataStore name remains unchanged.
- Existing recording history keys remain readable.
- Add a schema version field or key for the new podcast-session shape.
- Add a recording-segments key family for session recording segment metadata.
- Avoid moving transcript text out of the existing session record in this change.
- Encode new models as JSON using structured helpers, matching the existing Repository style.

Old data migration strategy:

- Existing `RecordingSessionEntity` records are read without destructive migration.
- When exposed as podcast sessions, old records map to one `PodcastSession`.
- Existing transcript segments and speaker profiles stay under the same session id.
- If an old record has transcript text or transcript segments but no recording segment metadata, synthesize a single logical legacy recording segment in memory or persist it lazily only after the session is modified.
- Existing statuses map as follows:
  - `NOT_STARTED` -> `DRAFT`
  - `CAPTURING_AUDIO`, `VAD_DETECTING`, `DIARIZING`, `TRANSCRIBING` -> `ERROR` on startup recovery, preserving the old interruption behavior
  - `SUMMARIZING` -> `ERROR` on startup recovery
  - `COMPLETED` with summary -> `SUMMARIZED`
  - `COMPLETED` without summary and with transcript -> `READY_FOR_SUMMARY`
  - `COMPLETED` without transcript -> `PAUSED`
  - `ERROR` -> `ERROR`

### Repository API Design

Add a session repository surface, either by extending `Repository` conservatively or introducing `SessionRepository` backed by the same storage.

Required APIs:

- `createSession(title: String? = null, sourceType: AudioSourceType? = null): PodcastSession`
- `renameSession(sessionId: String, title: String): AppResult<PodcastSession>`
- `appendSegment(sessionId: String, sourceType: AudioSourceType, startedAt: Long): AppResult<RecordingSegment>`
- `updateSegment(segment: RecordingSegment): AppResult<RecordingSegment>`
- `updateStatus(sessionId: String, status: PodcastSessionStatus, errorMessage: String? = null): AppResult<PodcastSession>`
- `observeSessions(): Flow<List<PodcastSession>>`
- `observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?>`

`PodcastSessionDetail` should aggregate:

- `session: PodcastSession`
- `recordingSegments: List<RecordingSegment>`
- `transcriptSegments: List<TranscriptSegmentEntity>`
- `speakerProfiles: List<SpeakerProfileEntity>`

### Error Recovery

Recording process killed while active:

- Startup recovery must find sessions with active recording states.
- Clear `activeSegmentId`.
- Mark the unfinished recording segment as `INTERRUPTED` when segment metadata exists.
- Mark session as `PAUSED` if completed prior data exists and the session can continue, otherwise `ERROR`.

Segment file exists but metadata incomplete:

- Preserve the segment row with `INTERRUPTED` or `ERROR`.
- Do not delete audio/PCM paths in this change.
- Store a bounded error message without sensitive full paths in logs.

Transcript exists but summary failed:

- Preserve transcript and previous summary text.
- Keep summary status failed or session status error only for the summary operation.
- Do not mark recording segments failed due to summary failure.

### Testing Design

State machine tests:

- Creation starts as draft.
- Start/pause/resume/finalize transitions are allowed only from expected states.
- Error transition preserves identifiers and segment membership.
- Recovery clears active segment.

Repository persistence tests:

- Create multiple sessions and observe sorted session list.
- Append multiple recording segments to one session.
- Rename session without changing segments/transcript/summary.
- Persist and reload sessions, recording segments, transcript segments, and speaker profiles.

Migration tests:

- Old session JSON fields remain readable.
- Old transcript segments and speaker profiles remain associated.
- Old completed sessions map to summary-ready or summarized status.
- Old interrupted statuses recover to non-recording state.

## Risks / Trade-offs

- Preferences DataStore may become too large for long multi-session transcripts -> Keep current storage for compatibility now and leave Room migration as a future change.
- Mapping old `COMPLETED` records to the new lifecycle may be ambiguous -> Use transcript/summary presence to derive the least surprising status.
- Introducing new enums can break persisted reads if names change -> Add tests for enum string names before implementation.
- Synthesized legacy segments could confuse future file cleanup -> Mark legacy segments clearly and avoid deleting files in this change.
- Repository may grow too large -> Prefer a focused `SessionRepository` facade if extending `Repository` makes boundaries unclear.

## Migration Plan

1. Add model and encode/decode tests before changing write paths.
2. Add read compatibility for existing sessions.
3. Add new recording segment storage without removing existing transcript segment storage.
4. Add recovery behavior for new active session/segment fields.
5. Run OpenSpec validate and Gradle unit tests.

Rollback:

- Because existing keys remain readable and old records are not destructively migrated, rollback should still be able to read old session data.
- New recording segment metadata may be ignored by older builds.

## Open Questions

- Should legacy logical recording segments be persisted immediately during startup recovery or only when the session is next modified?
- Should `recordingSegmentId` be added to transcript segments in this change or deferred until segmented recording writes real segment ids?
- Is Preferences DataStore acceptable for long-term transcript storage, or should a later Room migration become its own OpenSpec change?
