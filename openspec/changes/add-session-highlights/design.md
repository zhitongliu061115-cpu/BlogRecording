## Context

Structured summaries can produce quote candidates, but users need an explicit persisted favorite state. Highlights should remain tied to a single podcast session.

## Goals / Non-Goals

**Goals:**

- Generate and display candidate highlights for one session.
- Let users favorite/unfavorite highlights and persist that choice.
- Preserve favorited highlights across regeneration.

**Non-Goals:**

- Social sharing, global favorites page, cross-session search, manual transcript clipping, or quote image generation.

## Decisions

- Store generated candidates and user favorites in the session detail data.
- Include optional source start/end time and transcript segment IDs when available.
- Treat user-favorited highlights as durable. Regeneration may update generated candidates but must not delete favorites unless the user removes them.
- Use stable content/source fingerprints to match regenerated candidates to existing favorites when possible.

## Risks / Trade-offs

- AI-generated quotes may not match transcript exactly -> include source metadata and prefer transcript-derived text when available.
- Regeneration can duplicate items -> deduplicate by normalized text and source range.
- Detail UI can get long -> show highlights as a compact section.

## Migration Plan

- Existing sessions start with no highlight candidates or favorites.
- Future export can read the same highlight model.
