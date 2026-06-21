## Why

Users want to import podcast episodes from shared links instead of downloading files manually. The first URL MVP should prioritize Xiaoyuzhou episode URLs such as `https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185`, because this is the concrete user workflow to support.

## What Changes

- Add a URL import entry for podcast episode links.
- Prioritize Xiaoyuzhou episode URL recognition and media discovery.
- Reuse local imported-media processing once an audio URL or cached media file is resolved.
- Support direct media URLs and RSS enclosure URLs as secondary paths.
- Show clear errors for unsupported pages, network failures, blocked/private episodes, empty responses, and oversized downloads.
- Keep URL import privacy-safe: no API keys, cookies, full transcript text, raw audio, or private cache paths in logs.

## Capabilities

### New Capabilities

- `url-media-import`: URL validation, Xiaoyuzhou episode import, direct-media import, RSS enclosure import, download, and network error handling.
- `session-content-import`: Shared imported-content behavior reused after URL media resolution.
- `api-and-logging-privacy-boundary`: Privacy requirements for URL import network requests and logging.

### Modified Capabilities

- None. Existing completed change specs have not been archived under `openspec/specs/`, so this change records the needed delta files locally.

## Impact

- Affects URL import UI, network client behavior, media resolution, temporary private cache handling, import/transcription integration, tests, and manual network verification.
- Does not add login, Cookie handling, DRM bypass, anti-scraping bypass, subscription management, or arbitrary JavaScript web scraping.
