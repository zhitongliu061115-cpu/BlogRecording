## Harness Plan

- `openspec.cmd validate add-ai-bottom-nav`
- `git diff --check`
- `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.ui.UiNavigationPolicyTest" --tests "com.example.blogrecording.ui.AiChatUiStateMapperTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.qa.SessionQaUseCaseTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Manual Checks

- Open the app and confirm bottom tabs are 首页, AI, 我的.
- Open AI for the first time and confirm horizontally swipeable podcast cards appear.
- Select a podcast and confirm chat bubbles appear with assistant on the left and user messages on the right.
- Tap the top-right new conversation action and confirm the card chooser appears again.
- Open 我的 and confirm Settings and History remain reachable.

## Results

- 2026-06-21 DeepSeek QA reuse update:
  - `openspec.cmd validate add-ai-bottom-nav`: passed.
  - `git diff --check`: passed.
  - `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.ui.AiChatUiStateMapperTest" --tests "com.example.blogrecording.ui.UiNavigationPolicyTest" --tests "com.example.blogrecording.qa.SessionQaUseCaseTest"`: passed.
  - `.\gradlew.bat compileDebugAndroidTestKotlin`: passed.
  - `.\gradlew.bat testDebugUnitTest`: passed.
  - `.\gradlew.bat assembleDebug`: passed.
  - Manual device inspection: not run because no connected adb device or emulator was available.

- `openspec.cmd validate add-ai-bottom-nav`: passed.
- `git diff --check`: passed.
- `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.ui.UiNavigationPolicyTest" --tests "com.example.blogrecording.ui.AiChatUiStateMapperTest"`: passed.
- `.\gradlew.bat compileDebugAndroidTestKotlin`: passed.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat assembleDebug`: passed.
- Manual device inspection: not run because `adb devices` returned no connected devices or emulators.
