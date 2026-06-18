## ADDED Requirements

### Requirement: Changes are driven by OpenSpec artifacts
The project SHALL require proposal, design/specs, and tasks before implementation changes for this rebuild.

#### Scenario: Implementation starts
- **WHEN** code changes begin
- **THEN** `tasks.md` exists and OpenSpec status reports tasks as available

### Requirement: Each completed task updates task tracking
The project SHALL mark tasks complete in `tasks.md` as implementation progresses.

#### Scenario: Task is completed
- **WHEN** a task's implementation and verification finish
- **THEN** its checkbox is updated before commit

### Requirement: Harness checks run before commits and final delivery
The project SHALL run Git status, whitespace checks, OpenSpec status, unit tests, and debug build checks according to change phase.

#### Scenario: Implementation commit is prepared
- **WHEN** code or tests changed
- **THEN** `git status --short`, `git diff --check`, `testDebugUnitTest`, and relevant build checks are run or documented if environment-blocked

### Requirement: Commits remain small and Chinese-described
The project SHALL use small commits with Chinese messages in `类型：简短说明` format.

#### Scenario: A spec artifact is completed
- **WHEN** a spec artifact is ready
- **THEN** it is committed separately from implementation code
