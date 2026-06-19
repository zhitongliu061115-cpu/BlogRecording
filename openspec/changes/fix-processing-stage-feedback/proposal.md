## Why

The home card can start internal recording, but users cannot tell whether MediaProjection is authorized, whether system audio is actually being captured, whether PCM is only buffering, or whether ASR and summary jobs are running. The current 180-second transcription chunk also makes the first result arrive too late, and silent internal audio can be persisted as hallucinated short text.

## What Changes

- Add user-visible processing stages for internal recording authorization, capture, buffering, transcription, silence/unavailable-audio detection, pause/finalize, and summary generation.
- Shorten the default transcription chunk duration from 180 seconds to 30 seconds while keeping the existing bounds.
- Filter silent or non-speech chunks before SenseVoice recognition so repeated hallucinated snippets are not saved.
- Surface the same high-level stage in home cards, detail screen, and the foreground notification.
- Keep raw audio local and avoid logging raw PCM, transcript content, API keys, or device-private paths.

## Capabilities

### New Capabilities

- `processing-stage-feedback`: Covers visible stage/progress feedback for capture, transcription, summary, and silence handling.

### Modified Capabilities

- None.

## Impact

- Affects recording pipeline state in `AppViewModel`, home/detail UI state, foreground notification text, settings defaults, and unit/UI tests.
- No new external dependencies or storage migrations are required.
