## Why

Internal recording currently leads users into a confusing failure path that reads like a missing privileged system permission. The MVP needs to use Android's supported MediaProjection playback-capture flow so real-device testing can focus on capture, local transcription, and summary generation.

## What Changes

- Keep internal recording on Android's official `MediaProjection` + `AudioPlaybackCaptureConfiguration` path.
- Remove user-facing language that implies a privileged or system-signed audio-output permission is required.
- Preserve the microphone recording controls as an explicit fallback when a source app cannot be captured or emits silence.
- Make MediaProjection denial and silent internal audio recoverable, explainable MVP states.
- Document a focused harness and manual test path for internal recording, transcription, and summary.

## Capabilities

### New Capabilities
- `audio-capture-mvp`: Official Android internal-audio MVP behavior, permission messaging, silent-source handling, and microphone fallback.

### Modified Capabilities
- None.

## Impact

- Affected code: `MainActivity`, `AppViewModel`, processing-stage state, recording lifecycle policy, Home recording controls, and focused unit tests.
- Affected platform contracts: existing `RECORD_AUDIO`, Android 13+ `POST_NOTIFICATIONS`, foreground service, and MediaProjection flow remain in place.
- No new dependency, data migration, privileged permission, raw-audio upload, or secret handling change.
