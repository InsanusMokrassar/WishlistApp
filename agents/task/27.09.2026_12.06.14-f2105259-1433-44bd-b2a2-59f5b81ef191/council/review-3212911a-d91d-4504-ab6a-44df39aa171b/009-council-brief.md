Model: GPT-6; HL; no fallback
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/009-council-brief.md

# Frozen review-follow-up brief

Task: 27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191
Writer: preparation coordinator
Invocation: ordinary stage preparation; review continuation
Phase: proposals
Maximum voting cycles: 5
Repository revision: 1291075; branch feat/issue-79-user-email-change
Prior attempt: previous invocation was interrupted before participant outputs or commits; no retained output supplied.

## Task understanding and acceptance

The operator requests a correction to pending-email ownership in PR #82: pending email belongs to the external-users feature model, and User contains user information rather than email-change service state. Preserve established email-change behavior and the timestamp constraints from the original request and confirmed answers.

R1 / AC1: Remove pending-email service state from User and place pending-email state in the external-users feature model, consistently across production interfaces, repository behavior, client flows, serialization, tests and feature Architecture Notes as necessary.
R2 / AC2: Preserve relevant existing email-change behavior, including request/confirmation lifecycle, visible state, errors and repository integrity; participants identify exact existing behavior from current source.
R3 / AC3: Preserve the original rule that timestamps use korlibs DateTime with the MicroUtils serializer, existing compatibility constraints, and relevant configured/stored out-of-range handling. The operator delegated timestamp range policy to the full council; current source is evidence of established implementation. Do not expand this review into a fresh timestamp redesign without evidence that ownership correction requires a change.
R4 / AC4: Respect affected feature README Operator Notes; supply a complete intended Architecture Notes delta only.
R5 / AC5: Supply concrete implementation order, interfaces, risks, alternatives, external best-practice research, acceptance-linked test specifications for every planned change, and automated-coverage assessment. Proposed work and tests are not executed work.

## Confirmed inputs and allowed evidence

Read these exact immutable task inputs:
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/REVIEW-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-002.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/PROMPT.md

Current application source, configuration, tests, build files and feature READMEs are equally available to every role. Use ast-index for all code navigation; ast-index is installed at /home/linuxbrew/.linuxbrew/bin/ast-index. Read affected feature READMEs in full before analysis. Read source paths directly after ast-index navigation. No source or README edits are authorized.

Do not traverse task history or Git history for other report contents. Other historical artifacts are not supplied; request a specific needed excerpt through the coordinator. Exclude agents instruction files and agents/task history from repository searches before reading outputs. Do not read AGENTS.md, root workflow files, ordinary role files, other council role contracts, coordinator-only snapshots, or current-wave peer outputs, including copies in searches/history. Exposed excluded input is a process failure; stop and report.

## Constraints, exclusions and unknowns

The coordinator has no technical design vote and will collate participant-authored substantive sections. No unresolved operator question is currently identified from supplied review wording; each participant must explicitly identify every ambiguity discovered or state that no operator question remains. No implementation, unrelated refactoring, source edits, README edits, commits, pushes, further delegation, or persistent agent memory by participants.

The local capacity approval applies to this task: proposals and each voting wave run in two independent batches of two, each with identical frozen common inputs and no current-wave peer outputs. Batch 1: architect and designer. Batch 2: programmer and security. Collation begins only after all four outputs return. Capacity cleanup delays may require recorded retries.

## Frozen roster and model policy

- architect: agents/council/roles/ARCHITECT.md; frozen own contract agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/001-contract-architect.md
- designer: agents/council/roles/DESIGNER.md; frozen own contract agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/002-contract-designer.md
- programmer: agents/council/roles/PROGRAMMER.md; frozen own contract agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/003-contract-programmer.md
- security: agents/council/roles/SECURITY.md; frozen own contract agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/004-contract-security.md

Model policy: GPT-6.1-sol HL preferred; ML only if adequate HL unavailable with reason recorded; no LL.

Shared frozen instructions for every participant:
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/005-shared-COMMON.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/006-shared-PROTOCOL.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/007-shared-MODELS.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/008-shared-local.PROTOCOL.md

The complete roster IDs and paths are administrative membership evidence, not permission to read peer contracts. Each participant receives only the assigned contract.
