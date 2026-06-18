## 1. Transcript Aggregation

- [x] 1.1 Add a pure session transcript aggregation helper that orders transcript text by recording segment index and transcript start time; verify with unit tests.
- [x] 1.2 Preserve legacy transcript segments without `recordingSegmentId` in the aggregate; verify with legacy compatibility tests.
- [x] 1.3 Preserve prior transcript data when a later segment is missing, blank, or failed; verify with partial/failure tests.

## 2. Summary Eligibility And State

- [x] 2.1 Add a summary eligibility policy for NotReady, Ready, Summarizing, Summarized, and Failed states; verify with unit tests.
- [x] 2.2 Add repository helpers to persist summary status, bounded error messages, generated timestamp, and previous successful summary text; verify with repository tests.
- [x] 2.3 Ensure missing transcript and active recording block final summary; verify with policy tests.

## 3. Summary Lifecycle Use Case

- [x] 3.1 Add a session summary use case that reads aggregate transcript and starts DeepSeek summary only when eligible; verify with fake repository/client tests.
- [x] 3.2 Handle missing API key without sending a DeepSeek request; verify with fake API key store/client tests.
- [x] 3.3 Handle DeepSeek network/API errors by marking Failed and preserving prior summary text; verify with failure and retry tests.
- [x] 3.4 Preserve current chunk/map-reduce behavior for long transcripts and surface transcript-too-long failures when needed; verify with summary repository tests.

## 4. ViewModel And UI Wiring

- [x] 4.1 Wire Home card and detail summary actions to the session summary use case; verify with targeted ViewModel tests or fake boundary tests.
- [x] 4.2 Update summary labels/errors from persisted session summary status; verify with Home UI state mapper tests.
- [x] 4.3 Ensure no logs or user-visible errors include API keys, raw audio, PCM, full paths, or full transcript text; verify with code review and privacy tests where possible.

## 5. Verification

- [x] 5.1 Run `openspec.cmd validate add-session-summary-lifecycle`.
- [x] 5.2 Run targeted summary, transcription aggregation, repository, and Home mapper tests.
- [x] 5.3 Run `.\gradlew.bat testDebugUnitTest`.
- [x] 5.4 Run `.\gradlew.bat :app:assembleDebug`.
- [x] 5.5 Run `.\gradlew.bat :app:lintDebug`.
- [x] 5.6 Record manual QA for missing API key, successful summary, failed summary retry, previous summary preservation, no transcript, recording blocked, and partial transcript summary.

Manual QA note: no Android device or DeepSeek network credential was available in this environment, so real-device/manual flows were not executed. Covered equivalent lifecycle cases with fake repository/API-key/summary-client unit tests: missing API key does not send a request, successful summary persists, failed retry preserves previous summary, no transcript is blocked, active recording is blocked, and partial transcript aggregation keeps completed transcript text.
