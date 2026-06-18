## Why

Users do not always finish listening, recording, or reviewing a podcast in one sitting. BlogRecording needs a domain model that can represent one podcast session as a durable container for multiple recording segments, so users can pause, resume later, and still review one coherent session.

This change prepares the storage and state foundation before recording services, Home UI cards, or summary workflows are connected to segmented sessions.

## What Changes

- Introduce a podcast session domain concept that can contain multiple recording segments.
- Introduce durable recording segment metadata so each paused/resumed capture can be associated with the same podcast session.
- Add a session status lifecycle that can represent draft, recording, paused, processing, ready-for-summary, summarizing, summarized, and error states.
- Add repository operations for creating sessions, renaming sessions, appending/updating segments, updating status, observing session lists, and observing session details.
- Add persistence compatibility so existing recording history remains readable and is mapped into the new session model without data loss.
- Add recovery fields for interrupted active sessions and incomplete segment metadata.
- Add focused unit tests for the domain state machine, persistence, migration, and recovery behavior.

Non-goals:

- Do not implement Home podcast cards or any new capture UI.
- Do not implement actual AudioRecord or MediaProjection resume behavior.
- Do not implement DeepSeek session summary lifecycle.
- Do not change sherpa-onnx, SenseVoice, VAD, diarization, model loading, or bundled model verification behavior.

## Capabilities

### New Capabilities
- `recording-session`: Podcast session creation, rename, multi-segment membership, and session status lifecycle.
- `persistence-recovery`: Durable storage, old record compatibility, and process-restart recovery for podcast sessions and recording segments.

### Modified Capabilities
- None. `openspec/specs/` currently has no stable baseline specs; this change adds delta specs under the change directory.

## Impact

- Affected code areas:
  - `app/src/main/java/com/example/blogrecording/data/Models.kt`
  - `app/src/main/java/com/example/blogrecording/data/Repository.kt`
  - `app/src/main/java/com/example/blogrecording/data/RecordingPersistenceContract.kt`
  - `app/src/test/java/com/example/blogrecording/data/`
- Existing recording, transcription, summary, encrypted API key storage, and bundled model validation must continue to work.
- Backward compatibility:
  - Existing `RecordingSessionEntity` records remain readable.
  - Existing transcript segments and speaker profiles remain associated with their original record id.
  - Old single-record sessions are represented as podcast sessions with legacy-compatible metadata and, when needed, a single logical recording segment.
- Rollout plan:
  - First introduce the domain and persistence model behind tests.
  - Then connect segmented recording services in a later change.
  - Then expose Home cards in a later change.
  - Then add session-level summary lifecycle in a later change.

## Open Questions

- Preferences DataStore currently stores full transcript text and JSON arrays. It is acceptable for this MVP compatibility step, but large multi-segment transcripts may require a future Room migration.
- The recording segment model can store audio file references later, but this change should avoid requiring actual audio files until the recording-service change defines file creation and cleanup rules.
