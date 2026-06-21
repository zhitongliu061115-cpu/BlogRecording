## 1. OpenSpec

- [ ] 1.1 Create proposal, design, spec, and tasks for usage-based internal capture.
- [ ] 1.2 Validate the change and commit only the new OpenSpec artifacts.

## 2. Tests

- [ ] 2.1 Update internal audio policy tests to assert package UID targeting is not enabled by default.
- [ ] 2.2 Add tests that default usage filters still include media, game, and unknown playback.

## 3. Implementation

- [ ] 3.1 Remove automatic Cosmos package UID targeting from the default internal capture policy.
- [ ] 3.2 Keep explicit UID injection available for future targeted capture paths.
- [ ] 3.3 Ensure logs clearly show usage-based capture configuration.

## 4. Verification

- [ ] 4.1 Run OpenSpec validation, targeted unit tests, full unit tests, and debug build.
- [ ] 4.2 Record harness notes and the expected real-device retest path.
