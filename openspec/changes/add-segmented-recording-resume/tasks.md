## 1. Controller State Machine And Fakes

- [x] 1.1 Add fake recorder and fake session repository test fixtures for segmented recording controller tests; verify with targeted unit tests.
- [x] 1.2 Implement recording controller state transitions for start, pause, resume, switchSession, and finalize using fakes; verify with state machine unit tests.

## 2. Single Active Segment And Recovery

- [x] 2.1 Enforce the single active segment invariant and duplicate-action idempotency in controller logic; verify with concurrency/race unit tests.
- [x] 2.2 Implement process-restart recovery for active session and active segment metadata; verify with recovery unit tests.

## 3. Repository And Segment Metadata Integration

- [x] 3.1 Connect controller actions to `SessionRepository` active session and recording segment updates; verify with repository integration unit tests.
- [x] 3.2 Persist segment status transitions for recording, paused, completed, interrupted, and error states; verify with targeted persistence tests.

## 4. Microphone Capture Integration

- [x] 4.1 Map existing microphone start/stop flow to create or resume a podcast session with a single active segment; verify existing recording lifecycle tests still pass.
- [x] 4.2 Implement microphone pause/resume segment boundaries without completing the session; verify with fake recorder tests and compile checks.

## 5. System Audio Capture Integration

- [x] 5.1 Map existing MediaProjection start/stop flow to segmented sessions; verify permission-denied tests still pass.
- [x] 5.2 Handle MediaProjection cancellation or invalid permission on resume with an explainable recoverable error state; verify with targeted tests.

## 6. Transcription Association

- [x] 6.1 Associate transcript segments with the active recording segment id while preserving session transcript aggregation; verify with transcript unit tests.
- [x] 6.2 Preserve previous transcript data when one segment fails transcription; verify with failure-isolation tests.

## 7. Foreground Service Notification

- [x] 7.1 Extend foreground notification state to include podcast title, source, and recording state; verify with platform/contract tests.
- [x] 7.2 Add Pause, Resume, and Finish notification action contracts without starting duplicate segments; verify with controller tests.

## 8. Verification

- [x] 8.1 Run `openspec.cmd validate add-segmented-recording-resume`.
- [x] 8.2 Run `.\gradlew.bat testDebugUnitTest`.
- [x] 8.3 Run `.\gradlew.bat :app:assembleDebug`.
