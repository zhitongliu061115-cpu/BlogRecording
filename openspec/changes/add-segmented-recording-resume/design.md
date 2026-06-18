## Context

This change connects the podcast session domain to the recording service layer so a session can pause, resume, and switch recording segments. It does not add Home cards or session summary UI.

Current code evidence:

- `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt` owns the current recording orchestration. It creates one `RecordingSessionEntity` per recording run, starts `CaptureForegroundService`, owns `captureJob`, `activeCaptureManager`, and runs the PCM chunking, ASR, diarization, transcript assembly, and summary calls.
- `app/src/main/java/com/example/blogrecording/audio/AudioCaptureManager.kt` defines `start(): Flow<AppResult<PcmAudioStream>>` and `stop()`.
- `MicAudioCaptureManager.kt` uses `AudioRecord` microphone input and emits `PcmAudioStream`.
- `InternalAudioCaptureManager.kt` uses `MediaProjection` and `AudioPlaybackCaptureConfiguration`; it reports `MediaProjectionDenied` when projection is missing and `InternalAudioSilent` for repeated silent reads.
- `CaptureForegroundService.kt` starts the foreground notification but currently has no session title, active segment id, or pause/resume/finish actions.
- `PcmChunker.kt` chunks PCM by configured duration. `AppViewModel.runRecordingPipeline()` flushes the final chunk when capture stops.
- `TranscriptAssembler.kt` currently creates transcript segments using only a session id.
- `add-podcast-session-domain` introduced `PodcastSession`, `RecordingSegment`, `PodcastSessionStatus`, `RecordingSegmentStatus`, `SessionRepository`, `PodcastSessionDetail`, JSON codec, and migration/recovery helpers.

## Goals / Non-Goals

**Goals:**

- Enforce a single active recording segment across all podcast sessions.
- Implement pause as ending the current segment, not completing the whole session.
- Implement resume as appending a new segment to the same session.
- Implement switch session as pause active segment, then resume target session.
- Support both microphone and system-audio segmented capture.
- Associate transcript output with the active recording segment.
- Recover interrupted active segments after process restart.
- Update foreground service notification state and actions.

**Non-Goals:**

- Do not implement Home podcast cards.
- Do not implement session-level summary lifecycle.
- Do not change DeepSeek API, Keystore storage, SenseVoice model loading, VAD, diarization models, or `verifyBundledModels`.
- Do not upload audio, PCM, speaker embeddings, or full transcripts to any non-DeepSeek endpoint.

## Decisions

### Responsibilities

`RecordingController`

- New orchestration boundary for public recording actions.
- Owns a mutex or single-threaded dispatcher for `start`, `pause`, `resume`, `switchSession`, and `finalize`.
- Tracks `activeSessionId`, `activeSegmentId`, and active capture source.
- Talks to `SessionRepository` and the capture pipeline.

`RecordingService` / `CaptureForegroundService`

- Remains the Android foreground-service host.
- Receives notification state: session title, source type, active/paused state.
- Exposes notification actions: Pause, Resume, Stop/Finish.
- Does not own domain state transitions directly; actions delegate to the controller/ViewModel boundary.

`Repository` / `SessionRepository`

- Persists session and segment metadata.
- Provides recovery queries for active/interrupted state.
- Stores segment status and transcript association.

`AppViewModel`

- Keeps UI state and permission-result entry points.
- Delegates recording operations to `RecordingController`.
- Continues to expose existing microphone/internal start methods for backward compatibility.

### Active Id Lifecycle

- `activeSessionId` is set only after a session is chosen and model/permission gates pass.
- `activeSegmentId` is set only after `appendSegment()` succeeds.
- On pause, the controller stops `AudioCaptureManager`, flushes queued PCM, marks the segment completed or interrupted, clears `activeSegmentId`, and sets the session paused.
- On resume, the controller creates a new `RecordingSegment`; it never reuses a previous segment id.
- On finalize, any active segment is paused first, then the session becomes ready-for-summary.
- On process restart, persisted active ids are recovered by marking the active segment interrupted and clearing active state.

### Capture Strategy

Microphone:

- Continue using `MicAudioCaptureManager`.
- Each resume creates a fresh manager and fresh segment metadata.
- `stop()` ends only the active segment.

System audio:

- Continue using `InternalAudioCaptureManager`.
- Each resume requires a valid `MediaProjection`.
- If permission is cancelled or invalid, do not start a segment silently; mark the session recoverable with an explainable error.
- Stop MediaProjection when pausing a system-audio segment.

### File Naming

MVP does not require persisted audio files, but segment metadata must reserve stable names for future audio persistence.

- Session directory: `sessions/<sessionId>/`
- Segment prefix: `segment-<index>-<segmentId>`
- PCM: `sessions/<sessionId>/segment-<index>-<segmentId>.pcm`
- Final audio: `sessions/<sessionId>/segment-<index>-<segmentId>.wav`
- ASR input temp: `sessions/<sessionId>/segment-<index>-<segmentId>-asr.pcm`

Logs must not print full local paths; user-visible errors may show a bounded generic label.

### State Machine

| Action | Current state | Result |
| --- | --- | --- |
| pause | Recording | active segment completed/interrupted, session Paused |
| resume | Paused, ReadyForSummary, Error | new segment Recording |
| switchSession | any active Recording | pause active, then resume target |
| finalize | Recording | pause active, then ReadyForSummary |
| finalize | Paused | ReadyForSummary |
| capture failure | Recording | segment Error or Interrupted, session Error or Paused depending recoverability |
| recover | persisted active state | clear active ids, preserve completed data, mark interrupted segment |

### Foreground Notification

Actions:

- Pause: ends active segment and keeps session resumable.
- Resume: resumes the same session only if permission/source remains valid.
- Stop/Finish: pauses active segment if needed and finalizes the session.

Notification text:

- Title includes active podcast title.
- Body includes source type and whether a segment is recording, pausing, or processing final chunk.
- No full transcript, API key, or full audio path appears in notification text.

### ASR Segment Association

- Extend transcript assembly path to accept `recordingSegmentId`.
- Transcript segments produced during a chunk inherit the active recording segment id.
- Session aggregate transcript remains ordered by recording segment index and transcript time.
- If ASR fails for one segment, previous segment transcripts remain available.

### Error Handling

Permission cancelled:

- Do not create an active system-audio segment if MediaProjection is missing.
- Persist user-visible error on the target session.

File write failure:

- Mark active segment Error.
- Preserve session and completed segments.

ASR failure:

- Mark the segment Error or Processing failed.
- Preserve previous transcript.

Service killed:

- Recovery marks active segment Interrupted.
- Session becomes Paused when completed data exists, otherwise Error.

### Concurrency Control

- All public recording actions run under one mutex.
- Duplicate start/resume calls return the existing active state or a deterministic error.
- Switch session performs pause and resume as one serialized operation.
- Capture pipeline owns one active `AudioCaptureManager` at a time.
- `captureDispatcher = Dispatchers.IO.limitedParallelism(1)` can remain, but controller-level mutex is the domain gate.

### Testing

- Fake recorder for deterministic start/stop/failure.
- Fake repository for session/segment state.
- State machine tests for pause/resume/switch/finalize/recover.
- Process-death recovery tests for active segment and incomplete metadata.
- Compatibility tests that old single recording maps to one session + one segment.
- Targeted tests before real AudioRecord/MediaProjection integration.

## Risks / Trade-offs

- Existing `AppViewModel` is large -> Introduce `RecordingController` gradually and keep public ViewModel methods stable.
- MediaProjection cannot be reused after stop -> Require fresh permission for system-audio resume when needed.
- Pause while ASR is processing can race with segment completion -> Serialize state transitions and tag queued chunks with segment id.
- Old single-run recording must keep working -> Map old entry points to create session plus one segment.
- Preferences DataStore may be stressed by large transcript metadata -> Keep current storage for this change; Room migration remains future work.

## Migration Plan

1. Add fake recorder/controller tests.
2. Add controller and repository integration for active ids.
3. Map old start/stop methods to create single-session/single-segment behavior.
4. Integrate microphone resume.
5. Integrate system-audio resume and permission errors.
6. Add notification state/actions.
7. Run OpenSpec validate, unit tests, and debug build.

Rollback:

- Old recording flow remains reachable through existing ViewModel entry points.
- New segment metadata can be ignored by older builds; legacy session fields remain readable.

## Open Questions

- Should system-audio resume always request new MediaProjection permission, or can an active grant be reused within the same process when Android allows it?
- Should paused sessions with no transcript be finalizable, or should finalization require at least one completed segment?
- Should segment audio files be persisted in this change or left metadata-only until a dedicated export/playback change?
