## 1. OpenSpec

- [x] 1.1 Create proposal, design, spec, and task artifacts for the official Android internal-audio MVP.
- [x] 1.2 Validate the new OpenSpec change and commit only the new change artifacts.

## 2. Tests

- [x] 2.1 Add focused tests for MediaProjection denial messaging and non-privileged internal-audio permission policy.
- [x] 2.2 Add focused tests for silent internal-audio fallback guidance and microphone fallback availability.

## 3. Implementation

- [x] 3.1 Update user-visible authorization and denial text to describe screen/audio capture authorization instead of system-level audio-output permission.
- [x] 3.2 Keep card Start/Resume on official internal-audio capture while preserving microphone Start/Resume controls.
- [x] 3.3 Ensure silent or uncapturable internal audio stays recoverable and points users to microphone fallback without saving hallucinated transcripts.

## 4. Verification

- [x] 4.1 Run OpenSpec validation, targeted unit tests, and debug build.
- [x] 4.2 Record harness and manual MVP checklist results for this change.
