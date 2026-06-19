## 1. Specification

- [ ] 1.1 Validate the OpenSpec change and keep artifacts aligned with implementation.

## 2. Tests First

- [x] 2.1 Add unit coverage for processing-stage UI state mapping and summary blocking reasons.
- [x] 2.2 Add unit coverage for silent/non-speech chunk filtering before transcript persistence.
- [x] 2.3 Add unit coverage for the 30-second default transcription chunk setting.

## 3. Recording And Transcription

- [x] 3.1 Change the default transcription chunk duration to 30 seconds.
- [x] 3.2 Add structured processing-stage state updates for permission, capture, buffering, transcription, pause/finalize, and failures.
- [x] 3.3 Filter silent or non-speech chunks before SenseVoice recognition and skip transcript persistence for those chunks.

## 4. UI And Notification

- [x] 4.1 Display processing-stage feedback on home podcast cards and detail screen.
- [x] 4.2 Update foreground notification text with the current high-level stage in Chinese.
- [x] 4.3 Update real-device UI test assertions for the new status text.

## 5. Harness

- [x] 5.1 Run OpenSpec validation, unit tests, assemble/lint checks, and connected test when available.
- [x] 5.2 Record harness results and any device blockers in the change documentation.

## 6. Transcription Reliability Follow-up

- [x] 6.1 Replace hard VAD gating with conservative PCM-energy gating before ASR.
- [x] 6.2 Surface ASR attempt, empty result, and saved segment counts in processing feedback.
- [ ] 6.3 Validate the reliability follow-up with unit tests and assemble.
