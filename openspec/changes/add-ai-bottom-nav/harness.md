## Harness Plan

- `openspec.cmd validate add-ai-bottom-nav`
- `git diff --check`
- `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.ui.UiNavigationPolicyTest" --tests "com.example.blogrecording.ui.AiChatUiStateMapperTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Manual Checks

- Open the app and confirm bottom tabs are 首页, AI, 我的.
- Open AI for the first time and confirm horizontally swipeable podcast cards appear.
- Select a podcast and confirm chat bubbles appear with assistant on the left and user messages on the right.
- Tap the top-right new conversation action and confirm the card chooser appears again.
- Open 我的 and confirm Settings and History remain reachable.

## Results

- Pending.
