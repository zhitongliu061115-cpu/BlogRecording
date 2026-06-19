# Design

## Recording Flow
Card-level Start and Resume will request microphone and notification permissions first, then launch MediaProjection. After permission success, `AppViewModel` will start or resume the selected podcast session with an `InternalAudioCaptureManager`.

The existing `RecordingController` remains the source of truth for active segment metadata. `SegmentRecorder` receives `SegmentStartRequest` with `AudioSourceType.INTERNAL_AUDIO`, and `AppViewModel` supplies the valid MediaProjection result for that pending request.

## Home Card State
`HomeUiStateMapper` will sort details by `session.updatedAt` descending and expose only the latest 5 cards. It will map recent non-empty transcript segments to a display list per card, preserving full transcript data for Detail and History.

## UI
Each podcast card will render a small fixed-height vertical scroll window for transcript snippets. Empty sessions show a short empty state. Recording actions keep existing button labels but now represent system-audio capture.

## Testing
Use focused unit tests for mapping and recording controller behavior. Instrumentation tests should assert current visible text instead of stale pre-card UI labels.
