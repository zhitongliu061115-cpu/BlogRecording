## Why

When Home contains podcast cards, the Settings action can be pushed below the card list and become hard to find during urgent configuration or API key checks. Settings must remain directly reachable from the Home first viewport.

## What Changes

- Keep the Home-to-Settings navigation action visible near the Home title.
- Keep the Home-to-History navigation action beside Settings for consistent top-level navigation.
- Remove the duplicate bottom History and Settings buttons to avoid ambiguous UI actions.
- Do not change settings storage, API key handling, model settings, recording, import, transcript, summary, or URL behavior.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-navigation-and-privacy`: Home must expose the Settings action from the Home header/first viewport, even when podcast cards fill the scroll content.

## Impact

- Affected UI: `HomeScreen` header and Home navigation controls.
- Affected tests: Compose UI callback coverage for Home Settings navigation.
- No data model, permission, network, API key, or persistence changes.
