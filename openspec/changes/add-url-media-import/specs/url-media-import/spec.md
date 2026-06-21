## ADDED Requirements

### Requirement: Import Xiaoyuzhou Episode URL
The app SHALL support importing public Xiaoyuzhou single-episode URLs as the first URL-import MVP.

#### Scenario: Xiaoyuzhou episode URL is accepted
- **WHEN** the user enters a public Xiaoyuzhou episode URL such as `https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185`
- **THEN** the app recognizes it as a supported episode URL and attempts to resolve episode media

#### Scenario: Xiaoyuzhou episode media is resolved
- **WHEN** public episode metadata exposes a downloadable media resource
- **THEN** the app downloads or caches the media privately and creates an imported podcast session for transcription

#### Scenario: Xiaoyuzhou episode media cannot be resolved
- **WHEN** the public episode page does not expose a supported media resource
- **THEN** the app shows an unsupported-source message and does not attempt login, Cookie usage, DRM bypass, or browser automation

### Requirement: Import Direct Media Or RSS Enclosure URL
The app SHALL support direct media URLs and RSS enclosure URLs as secondary URL import sources.

#### Scenario: Direct media URL is accepted
- **WHEN** the user enters an HTTP or HTTPS URL that resolves to a supported audio or video media type
- **THEN** the app downloads the media into private cache and starts imported-media processing

#### Scenario: RSS enclosure is accepted
- **WHEN** the user enters a feed or item URL with a supported enclosure media URL
- **THEN** the app resolves the enclosure and starts imported-media processing

### Requirement: URL Import Failure Classification
The app SHALL classify URL import failures into recoverable user-facing states.

#### Scenario: Network request fails
- **WHEN** URL import encounters timeout, offline state, 401, 429, non-2xx, empty response, redirect failure, or oversized content
- **THEN** the app shows a clear URL import error and does not start ASR

#### Scenario: Unsupported URL is entered
- **WHEN** the user enters a URL outside Xiaoyuzhou, direct media, or RSS enclosure support
- **THEN** the app rejects it with an unsupported URL message
