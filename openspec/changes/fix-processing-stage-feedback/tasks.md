## 1. Specification

- [ ] 1.1 Validate the OpenSpec change and keep artifacts aligned with implementation.

## 2. Tests First

- [ ] 2.1 Add unit coverage for processing-stage UI state mapping and summary blocking reasons.
- [ ] 2.2 Add unit coverage for silent/non-speech chunk filtering before transcript persistence.
- [ ] 2.3 Add unit coverage for the 30-second default transcription chunk setting.

## 3. Recording And Transcription

- [ ] 3.1 Change the default transcription chunk duration to 30 seconds.
- [ ] 3.2 Add structured processing-stage state updates for permission, capture, buffering, transcription, pause/finalize, and failures.
- [ ] 3.3 Filter silent or non-speech chunks before SenseVoice recognition and skip transcript persistence for those chunks.

## 4. UI And Notification

- [ ] 4.1 Display processing-stage feedback on home podcast cards and detail screen.
- [ ] 4.2 Update foreground notification text with the current high-level stage in Chinese.
- [ ] 4.3 Update real-device UI test assertions for the new status text.

## 5. Harness

- [ ] 5.1 Run OpenSpec validation, unit tests, assemble/lint checks, and connected test when available.
- [ ] 5.2 Record harness results and any device blockers in the change documentation.
