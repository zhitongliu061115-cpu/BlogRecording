## Why

Users need to pause a podcast recording, resume it later, and switch between podcast sessions without losing transcript continuity. The domain model now exists, so the recording service layer needs to enforce one active segment and append new segments to the selected session.

## What Changes

- Add segmented recording service behavior for pause, resume, switch session, and finalize.
- Track active session and active segment identifiers while recording.
- Preserve existing microphone and system-audio entry behavior by mapping old single-run recording to one session with one segment.
- Associate ASR transcript output with the recording segment that produced it.
- Recover interrupted active recordings after process restart.
- Update foreground service notification state to reflect the active podcast title and recording state.

Non-goals:

- Do not implement Home podcast cards.
- Do not implement summary lifecycle or change DeepSeek behavior.
- Do not change SenseVoice, VAD, diarization, model loading, or bundled model verification.

## Capabilities

### New Capabilities
- `audio-capture`: Pause/resume capture behavior for microphone and system audio plus foreground notification state.
- `recording-session`: Active session switching, single active recording invariant, and interrupted active recording recovery.
- `transcription`: Segment-level transcript association and failure isolation.

### Modified Capabilities
- None. `openspec/specs/` currently has no stable baseline specs; this change adds delta specs under the change directory.

## Impact

- Affected code areas:
  - `app/src/main/java/com/example/blogrecording/ui/AppViewModel.kt`
  - `app/src/main/java/com/example/blogrecording/audio/`
  - `app/src/main/java/com/example/blogrecording/service/CaptureForegroundService.kt`
  - `app/src/main/java/com/example/blogrecording/data/`
  - `app/src/test/java/com/example/blogrecording/`
- Existing single recording flow, local ASR, summary generation, encrypted API key storage, and bundled model validation must keep working.
