## Why

Users already have downloaded podcast episodes, meeting recordings, and video clips that should be processed by the app without recording them again. Local import is the fastest MVP path because it avoids network variability while reusing the existing local transcription and summary pipeline.

## What Changes

- Add a local audio/video import entry from the app UI.
- Create podcast sessions from imported media with source metadata that does not expose private file paths.
- Copy or read selected media through Android SAF, extract audio when the source is video, and feed decoded audio into the existing transcription pipeline.
- Show import progress, cancellation, and recoverable errors for unsupported media, missing audio tracks, read failures, and user cancellation.
- Keep imported media in app-private storage only when needed for processing and never commit or log real media content.

## Capabilities

### New Capabilities

- `session-content-import`: Shared imported-content session lifecycle, metadata, status, and privacy contract.
- `local-media-import`: Local SAF media selection, validation, decoding, and import error handling.
- `transcription`: Requirements for imported media to enter the existing transcription path.
- `recording-session`: Requirements for imported content to create and preserve podcast session metadata.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects session data models, repository JSON compatibility, import UI, local media decoding, transcription orchestration, detail/home UI state, unit tests, and manual device verification.
- May use Android platform media APIs, but SHALL NOT add cloud upload or external media storage.
