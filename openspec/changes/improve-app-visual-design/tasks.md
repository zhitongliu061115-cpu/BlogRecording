## 1. Visual Foundation

- [x] 1.1 Add the Compose Material icons dependency and define stable light/dark product color schemes.
- [x] 1.2 Define the shared typography scale, shapes, spacing constants, and semantic status colors.
- [x] 1.3 Add reusable page chrome, section header, status pill, empty state, icon action, and settings-section components.
- [x] 1.4 Redesign bottom navigation with icons, stable dimensions, and accessible labels.

## 2. Home Workspace

- [x] 2.1 Redesign the Home header and consolidate capture/import sources into one action area.
- [x] 2.2 Redesign the empty state and session library heading without duplicate actions.
- [x] 2.3 Redesign podcast session cards around status, transcript progress, primary action, and quieter secondary actions.
- [x] 2.4 Redesign processing, model readiness, privacy, rename, URL import, and summary-style surfaces.

## 3. AI And Mine

- [x] 3.1 Redesign podcast selection cards, conversation bubbles, and the anchored AI composer.
- [x] 3.2 Redesign Mine readiness summary and navigation rows with semantic icons and status.

## 4. Review And Settings Screens

- [x] 4.1 Redesign History with page chrome, intentional empty state, and scannable session rows.
- [x] 4.2 Redesign Settings into API, summary, local processing, model, and privacy sections using suitable controls.
- [x] 4.3 Redesign Detail reading order, summary hierarchy, highlights, transcript, QA, utility actions, and separated delete action.

## 5. Tests And Verification

- [x] 5.1 Update focused UI tests and contract tests for the redesigned semantic controls and preserved callbacks.
- [x] 5.2 Run `openspec.cmd validate improve-app-visual-design` and confirm all implementation tasks are tracked.
- [x] 5.3 Run `testDebugUnitTest`, Android lint, Debug APK build, and instrumentation APK build.
- [x] 5.4 Install on the existing emulator, run connected UI tests, and capture light-theme screenshots for key screens.
- [ ] 5.5 Verify a compact viewport and dark theme for clipping, overlap, contrast, and stable navigation.

Manual QA note: compact portrait verification passed on `Medium_Phone_API_36.1`. Dark-theme screenshot verification remains pending because the final emulator UI-mode command was not approved.
