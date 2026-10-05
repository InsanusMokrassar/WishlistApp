Model: OpenAI GPT-5; HL coordinator
Changed files: agents/task/03.10.2026_07.24.07-71c06bc9-4774-4442-8226-94cc8d6157aa/council/attempt-001/001-council-brief.md

# Council brief

Task: prepare the merge of master `0b7bf58649a0a8844af710078d9d04c3cd75c1c4` into PR #81 head `c4ef55bd5886783bfc13c52e787b083505387b4e` in `/tmp/wishlist-sync-pr81-20261003-1322`.

Writer: Preparation coordinator, invocation `agent:main:subagent:964f143c-dc43-4fc0-ac80-de8a4d5adbf7`.

Inputs: the task `PROMPT.md`, shared council contracts, the four frozen role contracts, and the two supplied revisions.

## Frozen task understanding

- R1: use merge semantics to synchronize the existing PR implementation with current master; do not design a new feature.
- R2: preserve PR #81 email-authorized self-service password-change behavior.
- R3: preserve master's `Amount` and `EmailProfile` behavior.
- R4: resolve all merge conflicts and resulting build failures.
- R5: require a complete `./gradlew build` and PR-specific validation in the implementation plan.
- R6: do not merge, close, or push the PR from Preparation.
- R7: root alone may later recheck live PR state and ancestry and push the original branch.
- R8: Preparation writes planning/evidence only and changes no source code.
- R9: every council proposal and vote must be produced by a fresh, independent, parallel council wave using OpenAI models only, preferring Sol HL and permitting Terra ML only as the recorded fallback; ACP is prohibited.

Known repository evidence obtained before council dispatch:

- Worktree HEAD is exactly `c4ef55bd5886783bfc13c52e787b083505387b4e` on `oc/sync-pr81-20261003-1322`.
- `git merge-tree --write-tree --name-only c4ef55bd5886783bfc13c52e787b083505387b4e 0b7bf58649a0a8844af710078d9d04c3cd75c1c4` reports 13 content conflicts: `features/auth/README.md`, `features/email/README.md`, `features/email/server/src/commonMain/kotlin/Plugin.kt`, `features/ui/sidebar/src/commonTest/kotlin/ui/SidebarModelTest.kt`, `features/ui/users/README.md`, the Android/JS/JVM `ui/UserEditView.kt` files, `features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt`, `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt`, `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt`, `features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt`, and `features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelSaveTest.kt`.
- No source file has been modified by Preparation.

## Acceptance criteria

- A1: a self-contained plan gives exact conflict-resolution guidance while preserving R2 and R3.
- A2: planned verification includes the full build and focused password-change, email-profile, amount/profile, and affected UI/model regression coverage.
- A3: feature README deltas preserve every `## Operator Notes` section and document the merged architecture behavior.
- A4: no source edit, merge, push, PR close, or PR merge occurs in Preparation.
- A5: all four discovered council roles independently propose and then vote on an unchanged candidate in complete parallel waves.
- A6: only unanimous unconditional acceptance of one candidate can produce `CONSENSUS`.

## Constraints, exclusions, and unknowns

- The task is integration of existing implementation only. New feature scope is excluded.
- Routine conflict choices do not require operator questions; participants must derive them from repository evidence.
- Preparation must not author substantive architecture, implementation, testing, research, or README guidance itself; council participants must supply it.
- No operator question is currently identified from the supplied task. This does not waive participant-discovered questions.
- Maximum voting cycles: 5.
- Capability policy: Sol HL first; Terra ML only if Sol HL is unavailable, with the fallback reason recorded. No non-OpenAI or LL model is permitted.

## Frozen roster

The role directory contained exactly these four Markdown role contracts, each read in full; role IDs are unique and valid:

1. `architect` — `agents/council/roles/ARCHITECT.md`
2. `designer` — `agents/council/roles/DESIGNER.md`
3. `programmer` — `agents/council/roles/PROGRAMMER.md`
4. `security` — `agents/council/roles/SECURITY.md`

The complete contract contents remained coordinator-only. A participant would receive only the shared council documents, this brief, its own role contract, and its unique output allocation.

## Required proposal allocations

- `architect` -> `003-council-proposal-architect.md`
- `designer` -> `004-council-proposal-designer.md`
- `programmer` -> `005-council-proposal-programmer.md`
- `security` -> `006-council-proposal-security.md`

These allocations were reserved but not written. Dispatch feasibility is recorded in `002-council-launch.md`.
