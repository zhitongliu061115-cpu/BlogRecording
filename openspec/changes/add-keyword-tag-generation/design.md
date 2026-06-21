## Context

Tags can be generated after transcript or structured summary is available. They should help scanning without becoming a full taxonomy system.

## Goals / Non-Goals

**Goals:**

- Produce concise keywords and tags for one session.
- Normalize duplicates and cap tag length/count.
- Show tags on cards and details without disrupting existing actions.

**Non-Goals:**

- Global search, tag editing, tag deletion, tag folders, or cross-session topic analytics.

## Decisions

- Generate from structured summary when present, otherwise from transcript. This reduces context size and improves topic quality.
- Store tag text, normalized key, source, order, generated timestamp, and status.
- Apply deterministic normalization locally: trim whitespace, collapse duplicates case-insensitively where applicable, cap length, and keep original display text.
- Treat missing API key and empty transcript as blocked states, not failures.

## Risks / Trade-offs

- Generated tags may be noisy -> cap count and allow future regeneration.
- Multilingual duplicates can be hard -> keep simple normalization in MVP and preserve display text.
- Home cards can get crowded -> display only a short subset with overflow count.

## Migration Plan

- Existing sessions start with no generated tags.
- Tags can be generated lazily after summary or transcript availability.
