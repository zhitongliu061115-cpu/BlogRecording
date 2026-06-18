## 1. Home UI State And Mapper

- [ ] 1.1 Add `HomeUiState`, `PodcastCardUiState`, `RecordingActionState`, and rename dialog state models; verify with compile or targeted unit tests.
- [ ] 1.2 Implement a pure Home card state mapper from podcast/session detail inputs; verify labels, duration, segment count, transcription state, and summary state with unit tests.
- [ ] 1.3 Implement button availability rules in the mapper; verify start, pause, resume, finish, rename, summary-disabled, and switch-recording cases with unit tests.

## 2. Stateless Compose UI

- [ ] 2.1 Add stateless `PodcastSessionCard` and `HomeEmptyState` Composables; verify with Compose compile and Preview coverage.
- [ ] 2.2 Update `HomeScreen` to render the card list, empty state, active recording highlight, rename dialog, and existing navigation/model status; verify Compose compile.
- [ ] 2.3 Ensure all card buttons emit lambda events and do not access Repository, DataStore, SettingsStore, ApiKeyStore, or RecordingController directly; verify by code review and unit/compile checks.

## 3. ViewModel Event Wiring

- [ ] 3.1 Extend `AppUiState` or Home-specific state exposure so Home receives card UI state from ViewModel; verify state mapper tests and compile.
- [ ] 3.2 Add ViewModel handlers for create session and rename session; verify with unit tests or fake repository tests.
- [ ] 3.3 Add ViewModel handlers for start, pause, resume, and finish card actions using the existing segmented recording boundary; verify with fake controller/repository tests where possible.
- [ ] 3.4 Add ViewModel handler for start summary from a card, preserving missing-transcript and recording-disabled rules; verify with targeted unit tests.

## 4. Regression And Verification

- [ ] 4.1 Run `openspec.cmd validate add-home-podcast-cards`.
- [ ] 4.2 Run targeted Home UI state and ViewModel tests.
- [ ] 4.3 Run `.\gradlew.bat testDebugUnitTest`.
- [ ] 4.4 Run `.\gradlew.bat :app:assembleDebug`.
- [ ] 4.5 Record manual QA coverage for empty state, multiple cards, A/B recording switch, rename, pause/resume, finish, no-transcript summary disabled, and recording summary disabled.
