## Harness Results

Date: 2026-06-19

## Passed

- `git status --short`
- `git diff --check`
- `openspec.cmd validate fix-processing-stage-feedback`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat :app:lintDebug`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat assembleDebugAndroidTest`

## Device Test

- Command: `.\gradlew.bat connectedDebugAndroidTest`
- Result: blocked before tests started.
- Device/report: `PJD110 - Android 16`, device id reported as `16b8e473`.
- Failure: install of `app-debug-androidTest.apk` hit `ShellCommandUnresponsiveException`; cleanup then reported `device '16b8e473' not found`.
- Tests run: 0.

## Manual Verification Notes

- Full manual system-audio capture was not completed because connected Android test installation lost the device.
- Expected manual flow remains: create podcast, tap card start, authorize system audio, play capturable media, observe capture/buffering/transcribing/silence states, pause, confirm only non-silent transcript snippets persist, resume, finish, generate summary.

## Follow-up Harness Results

Date: 2026-06-19

## Passed

- `openspec.cmd validate fix-processing-stage-feedback`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat assembleDebugAndroidTest`
- Targeted unit/assemble runs for low-information ASR filtering, internal-audio usage capture policy, internal-audio unavailable feedback, and Compose entrypoint changes.

## Real-device Diagnosis

- Installed `app-debug.apk` successfully on `PJD110 - Android 16` (`16b8e473`).
- `dumpsys media_projection` showed active `TYPE_SCREEN_CAPTURE` for `com.example.blogrecording`.
- `dumpsys activity services com.example.blogrecording` showed `CaptureForegroundService` active with mediaProjection foreground type.
- Filtered logcat showed `AudioRecord` continuously reading buffers, but every read was all-zero PCM; chunk diagnostics reported `avgAmp=0` and `recognizerSegments=0`.
- User confirmed a podcast app was playing in the background during capture. The observed all-zero PCM means the source app/audio route did not expose audio to Android playback capture, not that SenseVoice failed to run.

## Device Test

- First rerun of `.\gradlew.bat connectedDebugAndroidTest`: tests started on device; 8 tests ran, 4 old UI assertions failed because the device retained manual-test app data and the UI now includes updated entrypoints.
- Updated `PodcastRecapUiTest` to avoid assuming an empty device state and to assert the new microphone fallback entrypoints.
- `.\gradlew.bat assembleDebugAndroidTest` passed after the UI test update.
- Second rerun of `.\gradlew.bat connectedDebugAndroidTest`: command timed out after 184 seconds in the harness environment. Gradle daemons were stopped and the app was force-stopped to avoid lingering device work.
