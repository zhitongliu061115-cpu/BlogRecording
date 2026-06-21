## Harness: fix-internal-audio-usage-capture

Date: 2026-06-21

## Real-device Log Diagnosis

- MediaProjection authorization completed.
- Internal audio foreground service started.
- AudioRecord initialized and entered recording state.
- Captured PCM remained all zeros and hit the silent-read path.
- Active playback was from `com.coloros.soundrecorder` UID `10298`.
- Installed Cosmos package UID `10314` was resolved as a preferred target, but it was not the active playback source.
- Root cause: default capture config used `addMatchingUid(10314)`, filtering out the actual playing app.

No raw audio, PCM samples, transcript text, API keys, or private files were recorded in this harness.

## Fix Verification Intent

Default internal recording should now build playback capture with usage filters:

- `USAGE_MEDIA`
- `USAGE_GAME`
- `USAGE_UNKNOWN`

The expected log marker is:

```text
capture_config mode=usages usages=1,14,0
```

If explicit UIDs are supplied in a future targeted path, the manager still supports:

```text
capture_config mode=matching_uids matchingUids=<uid-list>
```

## Commands

```powershell
openspec.cmd validate fix-internal-audio-usage-capture
git diff --check
.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.audio.InternalAudioCapturePolicyTest"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## Results

- `openspec.cmd validate fix-internal-audio-usage-capture`: passed.
- `git diff --check`: passed with CRLF normalization warnings only.
- `.\gradlew.bat testDebugUnitTest --tests "com.example.blogrecording.audio.InternalAudioCapturePolicyTest"`: passed.
- `.\gradlew.bat testDebugUnitTest`: passed.
- `.\gradlew.bat :app:assembleDebug`: passed.
- `connectedDebugAndroidTest`: not run in this pass.

## Manual Retest Path

1. Install the debug build on the device.
2. Start playback in a capturable media app that is not Cosmos.
3. Open BlogRecording and start internal recording.
4. Grant MediaProjection screen/audio capture permission.
5. Confirm logs show `capture_config mode=usages`.
6. Confirm logs eventually show `read_non_zero_first`.
7. Wait for a transcript chunk.
8. Finish the recording and generate the summary.
9. If logs keep showing `read_silence`, switch to another capturable source or use the microphone fallback.

## Expected Result

This fix removes the observed UID mis-filtering failure. It does not guarantee capture for apps that opt out of Android playback capture, produce silence, or route audio through unsupported paths.
