## Why

The current Android app has working behavior, but its implementation was created before the project adopted OpenSpec, SDD, Harness, and small Git commits. We need a traceable rebuild that preserves the same user-visible behavior while making future work spec-driven, test-protected, and easier to review.

## What Changes

- Rebuild the internal app structure inside the existing `:app` module with explicit boundaries for UI state, recording orchestration, local transcription, speaker labeling, summary generation, model provisioning, local data, and secrets.
- Preserve the existing application identity, package names, permissions, foreground service behavior, bundled model asset paths, DataStore names, SharedPreferences name, and Android Keystore alias.
- Document current behavior as OpenSpec capabilities before code changes, including known behavior such as configurable VAD settings while the main pipeline uses fixed PCM chunking.
- Add behavior-protection tests before and during refactoring so the rebuild can prove compatibility.
- Add or formalize Harness checks so each merge can run the same Git, OpenSpec, unit test, and debug build gates.
- Do not migrate model/AAR large-file history or change LFS policy in this change; record that risk for a separate change if needed.

## Capabilities

### New Capabilities

- `app-navigation-and-privacy`: Home, history, detail, settings, first-run privacy notice, visible errors, and navigation behavior.
- `audio-capture-session-lifecycle`: Microphone capture, internal audio capture, permissions, foreground service startup, stop/flush behavior, and error recovery.
- `local-transcription-pipeline`: PCM chunking, current VAD boundary behavior, SenseVoice recognition, and transcript segment assembly.
- `speaker-diarization-labeling`: Local speaker labeling, fallback behavior, unstable labels, and speaker profile summaries.
- `summary-generation`: DeepSeek API key use, transcript-only network boundary, prompt construction, chunked summary generation, and failure handling.
- `local-model-provisioning`: Bundled model asset verification, first-run private copy, model status reporting, and non-editable model paths.
- `settings-contract`: Settings DataStore preference keys, default values, persisted model paths, privacy acceptance, and clamped numeric ranges.
- `recording-history-persistence`: Recording DataStore keys, JSON field names, enum names, session ordering, segment/speaker cleanup, and old-record readability.
- `local-data-and-secrets`: Encrypted DeepSeek API key storage, SharedPreferences keys, Keystore alias, encryption algorithm, and key deletion behavior.
- `android-platform-contracts`: Gradle namespace/applicationId, launcher Activity, manifest permissions, foreground service declaration, notification contract, and backup/data extraction rules.
- `permission-and-platform-flow`: Microphone, notification, and MediaProjection permission flow, Android version differences, denied-permission errors, and failed-session persistence.
- `api-and-logging-privacy-boundary`: DeepSeek endpoint behavior, HTTP/JSON failure mapping, transcript-only network requests, and logging rules that avoid sensitive payloads.
- `build-assets-and-native-deps`: Bundled model assets, sherpa-onnx AAR dependency, `verifyBundledModels`, and assets-to-private-files copy contract.
- `development-governance-harness`: OpenSpec change workflow, task tracking, commit discipline, and repeatable Harness validation commands.

### Modified Capabilities

- None. `openspec/specs/` has no stable capability specs yet, so this change introduces baseline specs for existing behavior.

## Impact

- Affected code: `app/src/main/java/com/example/blogrecording/**`, focused on internal boundaries and orchestration.
- Affected platform/build files: `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/libs/*.aar`, and `app/src/main/assets/models/**`.
- Affected tests: unit and instrumentation tests under `app/src/test` and `app/src/androidTest`, expanded to protect existing behavior before refactoring.
- Affected build/governance: OpenSpec artifacts under `openspec/changes/refactor-sdd-harness-alignment/` and any local Harness scripts or Gradle verification needed for repeatable checks.
- Compatibility constraints: keep Kotlin package `com.example.blogrecording`, Gradle namespace `com.example.blogrecording`, `applicationId` `com.example.blogrecording`, `podcast_recap_settings`, `podcast_recap_records`, `deep_seek_key_store`, SharedPreferences keys `iv` and `cipher_text`, Keystore alias `podcast_recap_deep_seek_api_key`, encryption `AES/GCM/NoPadding`, current model asset paths, and existing app permissions unchanged.
- Upgrade constraints: old installations must still read settings and history, decrypt saved API keys, preserve recording ordering, and delete records with their segments and speaker profiles.
- Security and privacy constraints: do not commit API keys, tokens, certificates, signing files, local.properties, logs, real recordings, PCM/audio files, transcripts from real users, or raw speaker embeddings.
