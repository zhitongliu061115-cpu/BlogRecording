## Context

Session data is already available in detail views. Export should render the existing session detail into portable text formats without introducing a server or document-generation dependency.

## Goals / Non-Goals

**Goals:**

- Export one session at a time to Markdown, TXT, or JSON.
- Include transcript and available recap metadata in a deterministic order.
- Use Android system UI for save/share.

**Non-Goals:**

- PDF, Word, cloud sync, scheduled export, batch export, or public publishing.

## Decisions

- Implement format renderers as pure functions for unit testing.
- Markdown is the richest human-readable format; TXT is plain fallback; JSON is machine-readable with stable field names.
- Sanitize filenames from session title and generated timestamp.
- Treat user cancellation as neutral, not an error.
- Use system document/share flows so no broad storage permission is required for MVP.

## Risks / Trade-offs

- JSON schema can evolve -> include format version.
- Very long transcripts can produce large files -> stream or build carefully and verify memory use.
- System share targets differ by device -> manual device verification is required.

## Migration Plan

- No data migration is required.
- Export reads existing session detail and optional future metadata when available.
