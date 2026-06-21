## Why

Users need a clearer top-level route for chatting with the assistant about podcast content instead of entering QA from a detail screen only. The app also needs a simpler bottom navigation model that matches the requested Home, AI, and Mine structure.

## What Changes

- Replace the top-level bottom navigation surface with three tabs: 首页, AI, 我的.
- Add an AI chat page that uses a WeChat-like conversation layout for assistant replies and user messages.
- On first entry to AI, show horizontally swipeable podcast cards so the user can choose which podcast to chat about.
- Add a top-right new conversation action on the AI page; tapping it returns to the podcast card chooser for a new chat target.
- Keep existing Home recording/session behavior and existing Settings/History/Detail routes reachable through Home or Mine entry points.

## Capabilities

### New Capabilities
- `ai-bottom-navigation`: Covers bottom navigation labels, AI chat entry, podcast card conversation selection, and new-conversation behavior.

### Modified Capabilities
- None.

## Impact

- Affected UI state and navigation policy under `app/src/main/java/com/example/blogrecording/ui/`.
- Affected Compose surfaces: `PodcastRecapApp`, new AI chat screen, and Mine/profile-style top-level screen.
- Affected tests: navigation policy and AI UI state behavior tests.
- No new permissions, network endpoints, storage formats, API keys, or model assets.
