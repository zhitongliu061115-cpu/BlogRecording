## Context

The current `InternalAudioCaptureManager` chooses UID matching whenever `preferredCaptureUids()` resolves a package from `InternalAudioCapturePolicy.preferredCapturePackages`. Because Cosmos is installed, capture becomes `addMatchingUid(10314)`. Logs from the failing device session show Cosmos was paused while `com.coloros.soundrecorder` was the active `USAGE_MEDIA` player, so the app captured silence even though media was playing.

## Goals / Non-Goals

**Goals:**
- Make the MVP default capture all capturable media/game/unknown usage playback allowed by Android.
- Avoid automatic UID filtering from installed-but-inactive package preferences.
- Keep logs diagnostic enough to distinguish usage capture from UID capture without logging user audio or transcript content.

**Non-Goals:**
- Do not guarantee capture for apps that opt out of Android playback capture.
- Do not add privileged capture permissions or OEM-specific hooks.
- Do not implement a UI package picker in this change.

## Decisions

- Replace automatic preferred package UID matching with an empty default UID target list. This makes `InternalAudioCaptureManager` fall back to usage filters.
- Keep `preferredCaptureUids` constructor injection for future explicit tests or a future user-selected package flow.
- Keep `matchingUsages` unchanged so capture still follows Android's supported playback capture contract.

## Risks / Trade-offs

- Usage capture can include any capturable media audio while recording -> MVP behavior is broader but matches the user's expectation for "currently playing audio".
- Some sources will still return silence because they opt out or route through unsupported paths -> existing silent-audio recovery and microphone fallback remain.
- Removing Cosmos default targeting may affect targeted Cosmos-only experiments -> future targeting should be explicit, not automatic.

## Migration Plan

No data migration is needed. Ship as a normal app update and verify on device with the active playback app visible in `dumpsys audio` while internal recording runs.
