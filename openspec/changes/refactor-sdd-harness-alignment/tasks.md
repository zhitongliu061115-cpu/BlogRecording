## 1. Baseline Verification

- [x] 1.1 Run and record `git status --short`, `git diff --check`, and `openspec.cmd status --change "refactor-sdd-harness-alignment" --json`.
- [x] 1.2 Run current `.\gradlew.bat testDebugUnitTest` and record failures or environment blockers before code changes.
- [x] 1.3 Run current `.\gradlew.bat assembleDebug` and record failures or environment blockers before code changes.

## 2. Behavior Protection Tests

- [x] 2.1 Add tests for recording history DataStore compatibility: key families, JSON field names, enum names, ordering, delete cleanup, and interrupted-session recovery.
- [x] 2.2 Add tests for settings defaults and clamping: `max_speaker_count`, `vad_speech_threshold`, `transcription_chunk_duration_ms`, model path overwrite, and privacy acceptance.
- [x] 2.3 Add tests for bundled model provisioning and build asset contracts.
- [x] 2.4 Add tests for DeepSeek summary failure mapping, empty transcript handling, transcript chunking, and transcript-only prompt construction.
- [x] 2.5 Add tests for API key blank/save/delete behavior without exposing key material.
- [x] 2.6 Add focused tests or seams for recording lifecycle state transitions: permission denial, MediaProjection denial, foreground service failure, model gates, stop/flush, and recoverable internal silence.

## 3. Data, Settings, Models, and Secrets Rebuild

- [x] 3.1 Rebuild settings access behind a stable contract while preserving DataStore name, preference keys, defaults, and clamp behavior.
- [x] 3.2 Rebuild recording history persistence behind a stable contract while preserving DataStore name, JSON fields, enum names, ordering, startup recovery, and delete cleanup.
- [x] 3.3 Rebuild bundled model provisioning behind a stable contract while preserving asset paths, private copy location, status rules, and non-editable model paths.
- [x] 3.4 Rebuild API key storage behind a stable contract while preserving SharedPreferences name, `iv`, `cipher_text`, Keystore alias, and `AES/GCM/NoPadding`.

## 4. Recording and Transcription Rebuild

- [x] 4.1 Introduce recording orchestration boundaries that keep existing microphone/internal-audio flows and foreground service behavior.
- [x] 4.2 Preserve permission and platform flow for `RECORD_AUDIO`, Android 13+ `POST_NOTIFICATIONS`, and MediaProjection.
- [x] 4.3 Rebuild PCM chunking and recognition coordination while preserving chunk duration clamps and 30-second recognition windows.
- [x] 4.4 Preserve current VAD configuration behavior without changing the fixed-chunk main pipeline.
- [x] 4.5 Rebuild speaker labeling and profile updates while preserving disabled/fallback behavior and 60-second unstable fallback.

## 5. Summary and Privacy Rebuild

- [x] 5.1 Rebuild summary generation boundary while preserving DeepSeek endpoint, model setting, prompt styles, chunked summary flow, and final merge.
- [x] 5.2 Preserve error mapping for missing key, blank transcript, HTTP 401, HTTP 429, non-2xx, empty response, malformed response, and network failures.
- [x] 5.3 Preserve transcript-only request behavior and logging rules that exclude API keys, full transcripts, raw responses, audio, PCM, and speaker embeddings.

## 6. UI State and Platform Contracts

- [x] 6.1 Reconnect Home, Settings, History, Detail, and privacy notice to rebuilt boundaries without changing user-visible navigation or actions.
- [x] 6.2 Preserve Android platform contracts: namespace/applicationId/package, launcher activity, manifest permissions, foreground service declaration, backup rules, and data extraction rules.
- [x] 6.3 Update or add Compose/instrumentation coverage for first-run privacy notice, navigation, history detail opening, and delete behavior.

## 7. Harness and Finalization

- [x] 7.1 Add or document a repeatable local Harness entrypoint for Git checks, OpenSpec status, unit tests, and debug build.
- [ ] 7.2 Run `git status --short`, `git diff --check`, `openspec.cmd status --change "refactor-sdd-harness-alignment" --json`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat assembleDebug`.
- [ ] 7.3 Update this task list to mark completed tasks and record any environment-blocked checks in the final delivery notes.
