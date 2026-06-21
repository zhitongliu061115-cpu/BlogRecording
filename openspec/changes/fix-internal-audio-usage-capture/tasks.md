## 1. OpenSpec

- [x] 1.1 Create proposal, design, spec, and tasks for usage-based internal capture.
- [x] 1.2 Validate the change and commit only the new OpenSpec artifacts.

## 2. Tests

- [x] 2.1 Update internal audio policy tests to assert package UID targeting is not enabled by default.
- [x] 2.2 Add tests that default usage filters still include media, game, and unknown playback.

## 3. Implementation

- [x] 3.1 Remove automatic Cosmos package UID targeting from the default internal capture policy.
- [x] 3.2 Keep explicit UID injection available for future targeted capture paths.
- [x] 3.3 Ensure logs clearly show usage-based capture configuration.

## 4. Verification

- [x] 4.1 Run OpenSpec validation, targeted unit tests, full unit tests, and debug build.
- [x] 4.2 Record harness notes and the expected real-device retest path.
