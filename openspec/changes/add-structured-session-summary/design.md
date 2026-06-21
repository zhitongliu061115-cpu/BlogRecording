## Context

Session summary generation already aggregates transcript text and calls DeepSeek. Structured summary should extend that flow without making old summary records unreadable.

## Goals / Non-Goals

**Goals:**

- Produce machine-readable summary sections for UI, export, highlights, tags, and QA.
- Preserve the last successful summary and support retry/failure behavior.
- Fall back to plain text when the model returns non-JSON or partially malformed content.

**Non-Goals:**

- Timeline chapters, keyword tags, highight persistence, export, or QA UI.
- Changing the summary provider or requiring a new API key type.

## Decisions

- Store structured summary as optional fields next to the legacy text field. Legacy records without structured data remain valid.
- Prompt for strict JSON-like structure, but parse defensively because model output can drift.
- Use plain-text fallback by placing the raw safe summary text into the overview section when structured parsing fails.
- Keep section rendering data-driven so future timeline, tags, and highlights can reuse the same model.

## Risks / Trade-offs

- Model output may be malformed -> parser fallback keeps a usable summary and records a non-sensitive parse warning.
- Extra fields can grow storage -> keep sections concise and cap list lengths in generation policy.
- UI can become dense -> show structured sections as compact detail groups.

## Migration Plan

- Existing summaries read as legacy text with no structured sections.
- New summaries persist both display text and structured fields.
- Retry does not delete the previous successful summary until a new summary succeeds.
