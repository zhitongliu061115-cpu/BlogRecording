## Context

The app already has two capture managers: microphone `AudioRecord` and internal playback capture through `MediaProjection` plus `AudioPlaybackCaptureConfiguration`. Recent privileged-capture experiments were reverted, and the MVP should stay on the official Android route. The current weak spot is user-visible behavior: denied MediaProjection and all-zero PCM can look like missing system-level access instead of a normal Android permission or source-app capture limitation.

## Goals / Non-Goals

**Goals:**
- Keep the card Start/Resume default on official internal-audio capture.
- Keep microphone recording controls visible as a fallback.
- Make authorization, denial, silence, and fallback guidance clear in Chinese UI text.
- Verify the behavior with focused unit tests and OpenSpec validation before broad harness runs.

**Non-Goals:**
- Do not add `CAPTURE_AUDIO_OUTPUT`, `REMOTE_SUBMIX`, root, Shizuku, accessibility, or any system-signed capture path.
- Do not change local ASR, VAD, diarization, summary payload shape, or session persistence schema.
- Do not send raw audio, PCM, API keys, or full transcripts to non-summary endpoints.

## Decisions

- Use `MediaProjection` as the only internal-audio permission source. This is the supported Android API and matches the existing `InternalAudioCaptureManager`.
- Treat MediaProjection denial as a pre-capture authorization failure. The app should not create or continue a silent fake recording when authorization is cancelled or invalid.
- Treat repeated all-zero PCM as a recoverable source limitation. The session can keep waiting, but the UI must explain that the source app, route, or silence may be blocking capture.
- Preserve microphone controls rather than auto-switching. Automatic switching would mix internal and room audio without an explicit user decision.

## Risks / Trade-offs

- Some podcast apps opt out of playback capture -> explain the limitation and offer microphone fallback.
- Bluetooth, muted playback, or private audio routes can produce zero PCM -> keep the existing silence detection and surface a clear warning.
- Real-device MediaProjection behavior varies by Android version -> include manual QA in harness notes and keep unit tests focused on deterministic policy.

## Migration Plan

Implement as a normal app update. Existing sessions and stored transcript/summary data remain compatible. Rollback is a code revert of the UI policy and messaging changes; no data migration is required.
