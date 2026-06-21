## Context

Transcript segments already carry timing information. Timeline summary should convert those timestamps into user-facing chapters without requiring media playback controls.

## Goals / Non-Goals

**Goals:**

- Produce ordered chapters with stable time ranges and concise titles.
- Tie chapters back to transcript segment ranges for traceability.
- Degrade gracefully when timestamps are missing or invalid.

**Non-Goals:**

- Audio player seek controls, waveform UI, manual chapter editing, or speaker diarization improvements.

## Decisions

- Generate timeline context from transcript segments before calling the summary provider. This gives the model bounded timestamped chunks instead of an unstructured transcript blob.
- Validate model-returned chapter ranges after parsing. Invalid, overlapping, or reversed ranges are corrected when safe or rejected with fallback.
- Store chapters as part of recap data so export, highlights, and QA can reuse them later.
- If timestamps are missing, generate untimed sections and show them without start/end labels.

## Risks / Trade-offs

- ASR segment timestamps may be rough -> keep time ranges approximate and avoid promising frame-accurate navigation.
- Model may invent times -> validate against known transcript duration and segment bounds.
- Very long transcripts may exceed context -> summarize chunked transcript windows before final chapter generation.

## Migration Plan

- Existing structured summaries have no timeline chapters until regenerated.
- Existing sessions remain readable with an empty timeline list.
