## Context

The app currently creates podcast sessions from microphone or internal-audio recording segments. Local media import should become another source of session content while preserving the same downstream transcript, summary, detail, and home-card flows.

## Goals / Non-Goals

**Goals:**

- Represent imported local audio/video as a podcast session with sanitized source metadata.
- Use Android SAF for user-selected files and private app storage for temporary processing.
- Decode audio from audio or video files into the existing ASR input shape.
- Provide clear progress and failure states that can be unit-tested.

**Non-Goals:**

- Batch import, clip editing, cloud upload, media library sync, or user-visible raw file path management.
- Changing existing live recording behavior.

## Decisions

- Treat import as a content source, not a recording source. This keeps imported sessions independent from active microphone/internal recording lifecycles while allowing shared transcript and summary code.
- Store only sanitized metadata in persisted session records: display name, MIME type, duration when known, source kind, and import status. Full SAF URI strings and device paths must not appear in logs or user-visible errors.
- Decode locally before transcription. Video files are accepted only when an audio track can be extracted; files without usable audio fail before ASR.
- Keep unsupported-format and no-audio-track handling outside ASR so the transcription layer does not need to infer media container failures.

## Risks / Trade-offs

- Media decoding support differs by device -> expose format-specific failures and include manual mp3, m4a, and mp4 verification.
- Large files can be slow -> show import progress and keep processing cancellable.
- Persisted metadata changes can break old JSON -> add compatibility tests with legacy sessions.

## Migration Plan

- Add nullable/defaulted source metadata fields so old sessions read as recorded sessions.
- Keep existing session IDs, transcript, summary, and segment data unchanged.
- Rollback is safe if new imported sessions are ignored by older builds, but older builds may not understand the new source type.
