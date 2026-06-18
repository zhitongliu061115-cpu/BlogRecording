## Context

The app is an Android Kotlin and Jetpack Compose project for local podcast recording, transcription, speaker labeling, and DeepSeek recap generation. The rebuild must happen inside the existing `:app` module so installed users keep app identity, private settings, recording history, and encrypted API keys.

Current orchestration is concentrated in `AppViewModel`. The rebuild will introduce clearer internal boundaries while preserving all public platform, storage, network, model, and UI contracts captured in the specs.

## Goals / Non-Goals

**Goals:**

- Rebuild internal structure with explicit boundaries for UI state, recording orchestration, transcription, speaker labeling, summary generation, models, settings, history, and secrets.
- Preserve current user-visible behavior and upgrade compatibility.
- Add characterization tests before moving risky behavior.
- Keep every step traceable through OpenSpec tasks, small commits, and Harness results.

**Non-Goals:**

- No package/applicationId change.
- No new app module.
- No user-visible feature expansion.
- No migration of existing large-file Git history or LFS policy.
- No correction of the current VAD setting versus fixed-chunk pipeline mismatch unless a later OpenSpec change explicitly changes that behavior.

## Decisions

- Rebuild in place inside `:app` instead of creating a new app module. This keeps Gradle, manifest, resources, install identity, DataStore files, and Keystore entries compatible.
- Preserve storage contracts first, then refactor code behind them. DataStore names, preference keys, JSON fields, enum names, SharedPreferences keys, and Keystore alias are compatibility surfaces.
- Use characterization tests before moving orchestration. Repository, settings, model provisioning, summary failures, and recording-state transitions need protection before implementation changes.
- Split `AppViewModel` by orchestration responsibility, not by UI screen. UI screens should keep stable callbacks and state while domain coordinators handle recording, transcription, summary, and persistence work.
- Keep DeepSeek requests transcript-only. Raw audio, PCM, speaker embeddings, API keys, and full sensitive logs must not leave device or enter commits/log output.
- Treat OpenSpec tasks as the implementation queue. Each completed task updates `tasks.md`, runs the relevant Harness subset, and lands in its own commit.

## Risks / Trade-offs

- Data compatibility regression -> Add tests for existing DataStore keys, JSON field names, enum names, ordering, delete cleanup, and startup recovery.
- API key unreadable after rebuild -> Preserve SharedPreferences name, `iv`, `cipher_text`, Keystore alias, and `AES/GCM/NoPadding`; add tests around empty/save/delete behavior where platform test support allows.
- Recording lifecycle regression -> Characterize permission denial, MediaProjection denial, foreground service startup, stop/flush, and persisted error states.
- UI state drift -> Keep screen contracts stable and add Compose/instrumentation coverage for privacy dialog and main navigation.
- Model/build breakage -> Preserve asset paths, AAR dependency, and `verifyBundledModels`; run `assembleDebug`.
- Refactor scope creep -> Do not change behavior unless the current OpenSpec specs say so; any discovered behavior change requires spec/task update before code.
