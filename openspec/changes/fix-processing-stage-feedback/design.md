## Context

Current transcription uses local SenseVoice through sherpa-onnx. Audio is buffered by `PcmChunker` using `settings.transcriptionChunkDurationMs`, currently defaulting to 180 seconds. Each completed chunk is split into 30-second recognizer segments. A final partial chunk is flushed on pause or finish. The existing VAD classes exist, but the main recording pipeline sends chunk slices directly to ASR, so silent or uncatchable internal audio can still become short hallucinated text.

The UI mostly exposes a single status label and a few persisted session counters. This is not enough to explain the pipeline state to users, especially for internal audio where Android can legally return silent buffers when the source app does not allow playback capture.

## Goals / Non-Goals

**Goals:**

- Make every user-relevant phase visible: permission, capture, buffering, transcription, silence/unavailable audio, pause/finalize, summary readiness, summarizing, success, and failure.
- Reduce time-to-first-transcript by changing the default transcription chunk to 30 seconds.
- Stop saving silence hallucinations such as repeated "我." snippets when internal audio is silent or not capturable.
- Keep feedback available on both home cards and detail view, with matching foreground notification text.

**Non-Goals:**

- This change does not add cloud ASR or upload raw audio.
- This change does not implement word-by-word streaming transcription.
- This change does not redesign history browsing or summary content.

## Decisions

- Introduce a structured UI stage model rather than overloading one free-form VAD string. This lets cards, detail, and notification render the same state consistently.
- Keep local segmented ASR, but set the default chunk duration to 30 seconds. This preserves battery/performance characteristics while making progress visible fast enough for manual testing.
- Use a conservative PCM energy gate before SenseVoice. Exact or near-silent chunks are skipped, but VAD is not allowed to hard-block non-silent chunks because it can reject valid podcast/system audio and prevent any transcript from being attempted.
- Treat internal-audio silence as a recoverable capture state. Recording continues, but UI explains that the current app may not allow system capture or no audible media is playing.
- Do not persist raw stage logs or sensitive data. Persisted transcript remains only recognized user content.

## Risks / Trade-offs

- More frequent chunks increase ASR invocations. Mitigation: keep recognition serial on the existing recognition dispatcher and cap chunk duration with existing settings bounds.
- VAD may classify very quiet speech as silence. Mitigation: use existing thresholds/settings and show an explicit status instead of failing the session.
- Foreground notifications may not update for every second of buffering. Mitigation: update on meaningful stage transitions and coarse progress labels.
