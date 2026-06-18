## 1. Existing Data Structure Review

- [x] 1.1 Document current recording persistence fields in `app/src/main/java/com/example/blogrecording/data/RecordingPersistenceContract.kt` and verify with `.\gradlew.bat testDebugUnitTest --tests com.example.blogrecording.data.RecordingPersistenceContractTest`.
- [x] 1.2 Add or update characterization tests in `app/src/test/java/com/example/blogrecording/data/` for existing `RecordingSessionEntity`, `TranscriptSegmentEntity`, and `SpeakerProfileEntity` compatibility; verify with targeted data tests.

## 2. Domain Model Definition

- [x] 2.1 Add podcast session status, recording segment status, summary status, and domain entities in `app/src/main/java/com/example/blogrecording/data/Models.kt`; verify with model/state unit tests.
- [x] 2.2 Add pure state transition helpers for create/start/pause/resume/finalize/processing/ready/summarizing/summarized/error/recover in the data or domain layer; verify with state machine unit tests.

## 3. Repository API Definition

- [x] 3.1 Define the session repository surface for `createSession`, `renameSession`, `appendSegment`, `updateSegment`, `updateStatus`, `observeSessions`, and `observeSessionDetail`; verify by compiling data tests.
- [x] 3.2 Add `PodcastSessionDetail` aggregation for session, recording segments, transcript segments, and speaker profiles; verify with repository detail tests.

## 4. DataStore Schema, Version, And Migration

- [ ] 4.1 Extend `RecordingPersistenceContract.kt` with new schema/version and recording-segment contract fields while preserving existing key families; verify with contract tests.
- [ ] 4.2 Implement JSON encode/decode for new podcast session and recording segment metadata in the repository layer; verify with round-trip unit tests.
- [ ] 4.3 Implement process-restart recovery for active podcast sessions and incomplete recording segments; verify with recovery unit tests.

## 5. Backward Compatibility

- [ ] 5.1 Map existing recording records into podcast sessions without losing title, transcript, summary, status, timestamps, source metadata, transcript segments, or speaker profiles; verify with migration compatibility tests.
- [ ] 5.2 Ensure old completed records map to ready-for-summary or summarized states based on transcript and summary presence; verify with status mapping tests.
- [ ] 5.3 Ensure old interrupted recording statuses recover to a non-recording state with existing transcript data preserved; verify with interrupted-session tests.

## 6. Unit Tests

- [ ] 6.1 Add state machine tests covering allowed transitions, rejected transitions, idempotent recovery, and error preservation; verify with targeted state machine tests.
- [ ] 6.2 Add repository persistence tests covering multiple sessions, multiple recording segments per session, rename, status update, detail observation, and restart reload; verify with targeted repository tests.
- [ ] 6.3 Add migration tests covering existing DataStore JSON and transcript/speaker association compatibility; verify with targeted migration tests.

## 7. Build Verification

- [ ] 7.1 Run `openspec.cmd validate add-podcast-session-domain` and fix any spec/design/tasks drift.
- [ ] 7.2 Run `.\gradlew.bat testDebugUnitTest` and record any environment limitations if it cannot run.
- [ ] 7.3 Run `.\gradlew.bat :app:assembleDebug` to confirm `verifyBundledModels` and debug build still pass, or document the exact blocker.
