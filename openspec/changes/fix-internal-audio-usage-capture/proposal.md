## Why

Real-device logs showed MediaProjection authorization and AudioRecord startup succeeded, but internal capture returned all-zero PCM because capture was narrowed to the Cosmos package UID while the active playback source was another media app. The MVP should default to usage-based playback capture so currently playing media is not filtered out by a stale or unrelated package preference.

## What Changes

- Default internal audio capture to Android usage filters (`USAGE_MEDIA`, `USAGE_GAME`, `USAGE_UNKNOWN`) instead of automatically matching preferred package UIDs.
- Keep the policy explicit that target-package UID capture is not the default MVP path.
- Preserve the existing silent-audio recovery behavior when the source app or audio route still cannot be captured.
- Add focused tests and harness notes for the usage-capture fallback.

## Capabilities

### New Capabilities
- `internal-audio-usage-capture`: Default usage-based internal playback capture and diagnostics for avoiding package UID mis-filtering.

### Modified Capabilities
- None.

## Impact

- Affected code: `InternalAudioCapturePolicy`, `InternalAudioCaptureManager`, and focused audio policy tests.
- No manifest, permission, storage, ASR, VAD, summary, or data schema changes.
- No raw audio, PCM, transcript, API key, or device-private path is logged or uploaded.
