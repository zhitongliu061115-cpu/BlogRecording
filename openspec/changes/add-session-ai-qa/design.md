## Context

The app already stores transcripts and summaries per podcast session and has DeepSeek API key storage. QA should reuse those pieces while strictly avoiding cross-session context leakage.

## Goals / Non-Goals

**Goals:**

- Answer questions about one session using only that session's content.
- Persist QA history under the session.
- Classify blocked and failed states clearly.
- Keep prompts, logs, and errors privacy-safe.

**Non-Goals:**

- Cross-session search, web search, streaming responses, voice input, external vector database, or global chat history.

## Decisions

- Build context in priority order: structured summary, timeline chapters, favorited highlights, tags, transcript excerpts. Transcript is truncated deterministically when needed.
- Store user question, assistant answer, timestamps, status, model name, and sanitized error.
- Do not include other sessions, app logs, API keys, or private paths in QA context.
- Treat missing API key and missing session content as blocked states before network request.

## Risks / Trade-offs

- Long transcripts may exceed model limits -> deterministic truncation and summary-first context reduce size.
- Answers may overreach -> prompt must instruct the model to say when the answer is not in the episode.
- QA history may contain user-entered sensitive text -> keep it private to the session store and avoid logging question/answer bodies.

## Migration Plan

- Existing sessions start with empty QA history.
- Deleting a session removes its QA history with the session detail data.
