## ADDED Requirements

### Requirement: Structured Summary Supports Timeline Context
Structured summary data SHALL allow timeline chapters to coexist with overview, key points, action items, questions, and quote candidates.

#### Scenario: Summary includes timeline chapters
- **WHEN** timeline chapter generation succeeds for a structured summary
- **THEN** the structured summary stores chapters without removing existing overview, key points, action items, open questions, or quote candidates

#### Scenario: Timeline generation fails
- **WHEN** timeline generation fails after a structured summary already exists
- **THEN** the existing structured summary remains available and the timeline failure is recorded separately
