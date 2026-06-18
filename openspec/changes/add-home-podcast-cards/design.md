## Context

Current code evidence:

- `app/src/main/java/com/example/blogrecording/ui/HomeScreen.kt` currently renders a single global recording panel, global start/stop/summary buttons, model status, transcript preview, and privacy text. It receives `AppUiState` and callbacks; it does not yet render independent podcast session cards.
- `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt` currently owns Home state through `MutableStateFlow<AppUiState>`, combines `SettingsStore` and `repository.sessions`, exposes `startMicrophoneRecording()`, `startInternalRecording()`, `stopRecording()`, and `generateSummaryForCurrent()`, and stores `currentSession` as legacy `RecordingSessionEntity`.
- `app/src/main/java/com/example/blogrecording/ui/state/AppUiState.kt` currently contains legacy fields: `sessions: List<RecordingSessionEntity>`, `currentSession`, `currentSegments`, `selectedSessionId`, `recordingStatus`, `audioSourceType`, and `isGeneratingSummary`.
- `app/src/main/java/com/example/blogrecording/data/Models.kt` already contains the segmented domain types `PodcastSession`, `RecordingSegment`, `PodcastSessionStatus`, `RecordingSegmentStatus`, `SummaryStatus`, and `PodcastSessionDetail`.
- `app/src/main/java/com/example/blogrecording/data/SessionRepository.kt` exposes create, rename, append segment, update segment, update status, observe sessions, and observe session detail operations for podcast sessions.
- `add-segmented-recording-resume` introduced `RecordingController` and tests for start, pause, resume, switch session, finalize, recovery, and notification actions.

## Goals / Non-Goals

**Goals:**

- Represent the Home screen as a list of podcast session cards.
- Keep Home Composables stateless where practical: they receive UI state and emit callbacks only.
- Derive button enablement from `PodcastCardUiState` rather than duplicating domain state-machine logic in Composables.
- Support create, rename, start, pause, resume, finish, and start summary events from cards.
- Keep only one podcast visually marked as Recording.
- Provide empty state and create-session entry when no sessions exist.
- Preserve existing top-level navigation to History, Detail, and Settings.

**Non-Goals:**

- Do not change `AudioRecord`, `MediaProjection`, `CaptureForegroundService`, ASR, VAD, diarization, bundled model verification, or DeepSeek client behavior.
- Do not implement the session summary lifecycle beyond dispatching the existing summary action from card state.
- Do not move persistence reads into Composables.
- Do not archive or sync specs in this change.

## Decisions

### UI state data structures

Add Home-specific UI state that is built in the ViewModel or a pure mapper:

```kotlin
data class HomeUiState(
    val cards: List<PodcastCardUiState>,
    val isEmpty: Boolean,
    val activeRecordingSessionId: String?,
    val renameDialog: RenameDialogUiState?,
    val errorMessage: String?
)

data class PodcastCardUiState(
    val sessionId: String,
    val title: String,
    val statusLabel: String,
    val durationLabel: String,
    val segmentCountLabel: String,
    val transcriptionLabel: String,
    val summaryLabel: String,
    val isRecording: Boolean,
    val actionState: RecordingActionState,
    val canRename: Boolean,
    val canFinish: Boolean,
    val canStartSummary: Boolean,
    val startSummaryDisabledReason: String?
)

data class RecordingActionState(
    val canStart: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val switchingFromAnotherSession: Boolean
)
```

`RenameDialogUiState` can hold `sessionId`, `initialTitle`, and current draft text if draft state remains in ViewModel. A local `rememberSaveable` draft is acceptable only for text-field editing, with confirm/cancel still emitted to ViewModel.

### UI events

Home UI events:

- `OnCreateSession`
- `OnRenameSession(sessionId, title)`
- `OnStartRecording(sessionId)`
- `OnPauseRecording(sessionId)`
- `OnResumeRecording(sessionId)`
- `OnFinishSession(sessionId)`
- `OnStartSummary(sessionId)`

Compose functions expose these as lambdas. The ViewModel translates them to repository/controller/summary operations.

### Button availability matrix

| Card state | Start | Pause | Resume | Finish | Rename | Start summary |
| --- | --- | --- | --- | --- | --- | --- |
| Draft, no active recording | enabled | disabled | disabled | disabled | enabled | disabled |
| Draft, another card Recording | enabled with switch semantics | disabled | disabled | disabled | enabled | disabled |
| Recording | disabled | enabled | disabled | enabled | enabled | disabled |
| Paused with segments | disabled | disabled | enabled | enabled | enabled | enabled only when transcript exists |
| Ready for summary | disabled | disabled | enabled | disabled | enabled | enabled when transcript exists |
| Summarizing | disabled | disabled | disabled | disabled | disabled or enabled only if safe | disabled |
| Summarized | disabled | disabled | enabled | disabled | enabled | enabled for retry only if summary lifecycle later allows |
| Error recoverable | disabled | disabled | enabled | depends on completed segments | enabled | enabled only when transcript exists and not recording |

The mapper owns this matrix. UI only renders `enabled` flags and optional helper labels.

### Compose state lifting

- `HomeScreen` receives `HomeUiState`, model status, navigation callbacks, and event callbacks.
- `PodcastSessionCard` receives a single `PodcastCardUiState` plus callbacks.
- `HomeEmptyState` receives `onCreateSession`.
- Composables MUST NOT read `Repository`, `SessionRepository`, `SettingsStore`, `DataStore`, `ApiKeyStore`, or `RecordingController` directly.

### Rename dialog state management

ViewModel owns whether the rename dialog is open and which session is being renamed. The dialog confirms through `OnRenameSession`; blank titles are either rejected with a UI error or normalized by the ViewModel to keep the previous title. Cancel closes the dialog without repository mutation.

### Navigation to detail strategy

Clicking the card body opens detail for that session via the existing navigation boundary. If detail still uses legacy `RecordingSessionEntity`, the ViewModel maps or selects the matching legacy-compatible session until a later detail-page change migrates to `PodcastSessionDetail`.

### Error snackbar/dialog strategy

ViewModel converts `AppError` or deterministic domain errors into bounded user-facing text. Snackbars are preferred for recoverable action errors such as missing permissions, missing transcript, or invalid action. Dialogs are reserved for destructive confirmations if future UX needs them. No snackbar/dialog text may include API keys, full audio paths, raw PCM, or full transcript text.

### UI tests and manual QA

Unit tests:

- Pure mapper tests for card labels, single recording highlighting, and button matrix.
- Event tests with fake ViewModel/controller/repository boundaries for create, rename, start, pause, resume, finish, and summary dispatch.
- Regression tests that summary is disabled without transcript and while recording.

Compose/manual QA:

- Empty state shows create entry.
- Multiple sessions render as independent cards.
- A recording card is visually highlighted.
- Resume B while A records displays/dispatches switch semantics.
- Rename dialog updates only the target card.
- Summary action is disabled without transcript and while recording.

## Risks / Trade-offs

- `AppViewModel` currently mixes legacy single-recording orchestration with new session concepts -> Use a Home mapper and keep implementation scoped so card UI can consume new state without rewriting the ASR pipeline.
- Existing strings appear partially mojibake in source files -> New UI strings should be added in readable UTF-8 only when editing files that already contain user-facing text, and tests should assert behavior rather than fragile localized text where possible.
- Detail screen may still expect legacy sessions -> Keep detail navigation compatibility in this change and defer full detail migration.
- Summary lifecycle is not part of this change -> Card can dispatch existing summary behavior, but richer retry/status semantics remain for `add-session-summary-lifecycle`.

## Migration Plan

1. Add pure Home UI state and mapper tests.
2. Add stateless `PodcastSessionCard`, empty state, and previews.
3. Wire `HomeScreen` to render cards from ViewModel-provided state.
4. Add ViewModel event handlers for create, rename, start, pause, resume, finish, and start summary.
5. Run OpenSpec validation, targeted unit tests, `testDebugUnitTest`, and `:app:assembleDebug`.

Rollback:

- The old Home global controls can remain behind the same ViewModel callbacks until card actions are stable.
- No persistence migration is required for this UI-only change.

## Open Questions

- Should card start/resume default to microphone, or should the UI expose source selection per action in this change?
- Should finished sessions allow resume from Home, or should resume require an explicit reopen action in a later change?
- Should summary retry be visible immediately or deferred to the summary lifecycle change?
