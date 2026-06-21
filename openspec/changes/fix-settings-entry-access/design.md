## Context

`HomeScreen` renders podcast cards inside a vertically scrolling column. The Settings button is currently in a lower action row after the empty/card content. When the card list is populated, users may need to scroll past cards before finding Settings.

## Goals / Non-Goals

**Goals:**
- Make Settings reachable from Home without scrolling through podcast cards.
- Preserve the existing `onNavigate(AppScreen.SETTINGS)` navigation path.
- Keep the fix scoped to Home layout and targeted UI coverage.

**Non-Goals:**
- Do not change Settings content or persistence.
- Do not change URL import, local import, recording, transcript, summary, export, QA, or API key storage.
- Do not introduce new navigation architecture.

## Decisions

- Move History and Settings into a title-row navigation group at the top of Home.
  - Rationale: the action becomes first-viewport and uses the existing navigation callback.
  - Alternative considered: duplicate another Settings button above cards. Rejected because duplicate labels can make tests and accessibility traversal ambiguous.
- Keep import/create actions in their existing contextual rows.
  - Rationale: this fix is about top-level navigation availability, not redesigning Home actions.

## Risks / Trade-offs

- [Risk] Header row could become crowded on narrow screens -> Mitigation: only two compact navigation buttons are added beside the title, while import/create actions stay in their existing rows.
- [Risk] Existing tests may find a different Settings node -> Mitigation: remove duplicate bottom Settings button and add a targeted Home callback test.

## Migration Plan

No data migration. Rollback is limited to reverting the Home layout change.
