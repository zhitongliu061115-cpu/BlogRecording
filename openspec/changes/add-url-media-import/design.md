## Context

Local import provides a reusable media-processing path. URL import should resolve a remote episode into a media resource, cache it privately, and then hand off to the same imported-content flow.

## Goals / Non-Goals

**Goals:**

- Accept Xiaoyuzhou single-episode links as the primary MVP URL shape.
- Resolve media in a deterministic, testable way without browser automation.
- Support direct audio/video URLs and RSS enclosure URLs after the Xiaoyuzhou path.
- Classify common network failures into user-facing states.

**Non-Goals:**

- Login-only episodes, private feeds, Cookie import, DRM bypass, JavaScript rendering, generic web scraping, or feed subscription management.
- Uploading media to a server.

## Decisions

- Use a URL resolver layer before download. Resolvers are tried in priority order: Xiaoyuzhou episode URL, direct media URL, RSS enclosure.
- Treat Xiaoyuzhou support as site-specific parsing of public episode metadata, not a general browser scraper. If the public page shape changes or media cannot be found, fail with an unsupported-source message.
- Download remote media into app-private cache and pass the cached item to the shared import pipeline. Persist only sanitized source URL metadata and episode title.
- Enforce size and timeout limits before transcription to prevent runaway downloads.
- Keep redirects allowed only for HTTP/HTTPS targets and sanitize all logged URLs.

## Risks / Trade-offs

- Xiaoyuzhou page structure may change -> isolate resolver tests with fixture HTML/metadata and fail safely.
- Some episodes may require app-only access -> show unsupported/private episode instead of attempting credential bypass.
- Large downloads may consume storage -> apply size limit and cleanup failed cache files.

## Migration Plan

- URL-imported sessions use the same nullable/defaulted imported-content metadata added by local import.
- Existing sessions are unaffected.
- URL import can be disabled independently if resolver behavior breaks without impacting local import.
