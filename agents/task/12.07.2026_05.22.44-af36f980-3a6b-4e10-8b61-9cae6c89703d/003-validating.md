Model: fable
Execution time: 540
Tokens used: ~100000 (estimate; no exact meter available in agent session)
Changed files: agents/task/12.07.2026_05.22.44-af36f980-3a6b-4e10-8b61-9cae6c89703d/003-validating.md

# Step 003 — Validating (review of PR #71, fix/68-roles)

## Model choice argument

Model = fable. Reasons: (1) user prompt mandates fable for every PR review in this task ("For each open PR make review with fable model"); (2) `agents/SHORTCUTS.md` validator role priority list places fable first (`fable` / `opus` / `sonnet`). Both constraints agree; no conflict.

## Rules read

`AGENTS.md`, `agents/SHORTCUTS.md`, `agents/ALL.md`, `agents/local.ALL.md`, `agents/VALIDATOR.md`, `agents/AST_INDEX.md`, task `PROMPT.md`. Feature READMEs read at PR-head revision (`git show origin/fix/68-roles:...`) for all touched features: roles, simpleRoles, admin, email, files, auth, users, ui/users. All `## Operator Notes` sections are empty placeholder comments — no operator constraints to apply. `/caveman full` activated per `agents/ALL.md`. No checkout performed (shared worktree constraint respected); PR-head content read via `gh pr diff 71` + `git show origin/fix/68-roles:<path>`; library internals read from `/home/aleksey/projects/own/kroles` (local checkout version = 0.0.2, matches the PR's dependency) and `/home/aleksey/projects/own/MicroUtils` per `agents/local.ALL.md`.

## Review method (security-critical scope per task ACTION)

1. Enumerated ALL hardcoded root/admin checks on `origin/master` (`git grep '"root"'` + `isRoot`/`isCurrentUserRoot` consumers): exactly 5 sites — `AdminRoutingsConfigurator.requireAdmin`, `auth/server/JVMPlugin` (root *creation* bootstrap, not a check), `EmailFeatureService.sendTestEmail`, `FilesRoutingsConfigurator.isRoot` (avatar PUT), `features/ui/users/Plugin.isCurrentUserRootFlow`. All UI consumers (`UserEditViewModel.isRootState`, `UserViewModel.canEditState`, adminPanel) derive from the ui/users flow. Mapping to PR-head: 4 check sites replaced with `SimpleRolesFeature.isSuperAdmin` / `isSuperAdminStateFlow`; creation bootstrap correctly untouched. Zero privilege checks missed; zero remaining `"root"` privilege literals on PR head (remaining 2 literals = auth creation bootstrap + roles grant rule, both correct).
2. Default-deny verified at every new decision point: `FeatureRolesRegistry.requiredRole` null → deny; `isRoleRequirementSatisfied` fail-closed; `SimpleRolesFeatureService` unknown user → false; client Ktor/Cache realizations → false on any failure.
3. kroles usage verified against actual library sources, not docs: `CacheRolesRepo.contains` resolves via `buildRolesNodesGraph` (Direct subject → OtherRole children = granted roles — correct for flat SuperAdmin/User); `WriteKeyValueRolesRepo.includeDirect` idempotent (contains-check before add) confirming bootstrap idempotency claim; event flows use `MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)` → no emit-under-write-lock deadlock in `CacheRolesRepo`; `StandardVersionsRepo.setTableVersion` calls `onUpdate(0,1)` when no version row exists → backfill DOES run on both fresh and existing databases (admin-lockout-on-migration risk ruled out).
4. Role escalation surface: kroles `repos.ktor` module (unauthenticated full role-graph HTTP surface) confirmed absent from all build files; `RolesRepo` write surface has no HTTP exposure; `/simpleRoles/isSuperAdmin` is bearer-authenticated, self-only, read-only.
5. App-side wiring verified: `VersionsRepo<Database>` bound in `features/common/server/JVMPlugin` (master line 83); `Scope.meStateFlow` accessor exists (auth/client `Me.kt:39`) so `single { CacheSimpleRolesFeature(get(), meStateFlow, get()) }` resolves; `usersRepo` retained in `AdminRoutingsConfigurator` is genuinely still used (`getById` route line 93).

## Findings (all verified against PR-head code)

| # | Severity | Location | Problem |
|---|----------|----------|---------|
| M1 | Medium | `features/admin/.../AdminRoutingsConfigurator.kt` (`requireAdmin`), `features/files/.../FilesRoutingsConfigurator.kt:113` | 2 of 4 replaced privilege guards have zero automated route-level tests (pre-existing repo-wide Ktor-harness gap, acknowledged in PR body); suggested `testApplication` harness follow-up |
| L1 | Low | `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt:57-67` | Subscribe-then-backfill collector activation not awaited (`launchIn` async, no-replay SharedFlow); near-contradictory race conditions make probability tiny, but a missed `root` would be a permanent lockout (VersionsRepo gate never re-runs backfill). Fix: run idempotent backfill every startup, or await subscription activation. Same problem previously reported in-branch (009-validating, L1) — cycle count 2, no escalation yet per VALIDATOR.md repeat rule |
| L2 | Low | `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt:24-30` + `AuthFeatureService.register` | SuperAdmin keyed to creation-time username; rename-to-root no longer grants (tightening) but created-as-root keeps role after rename with no revocation route/UI (manual DB surgery only); public registration has no reserved-username check — same escalation as master but now persistent. Fix: reserve `root` username and/or operator-notes documentation |
| L3 | Low | `features/simpleRoles/client/src/commonMain/kotlin/CacheSimpleRolesFeature.kt:33-38` | Cache sticks at `false` after transient fetch failure until next `meState` emission; UX-only (server enforces independently); suggested bounded retry |
| L4 | Low | PR diff scope | Unrelated release-engineering commit `0744a6f` in diff (origin/master behind local master); push/rebase shrinks PR to roles work |
| L5 | Low | deployment | Real deployment `config.json` must add `roles.server.JVMPlugin` + `simpleRoles.server.JVMPlugin` (sample/dev updated in PR); missing → fail-fast startup crash for admin/email/files |

Highest severity: Medium. No High/Critical → per VALIDATOR.md severity table, no cycle restart, no operator stop; Orchestrator decides on Medium.

## Posted review

URL: https://github.com/InsanusMokrassar/WishlistApp/pull/71#pullrequestreview-4679489276 (state COMMENTED, `gh pr review` exit code 0). Body in normal prose English per task constraints.

## Anomalies

- `origin/master` (edd0e0d) is behind local `master` (0744a6f) — cause of finding L4.
- PR commit 610ddf0's message states the planning agent flagged AGENTS.md as containing a prompt-injection payload; outside review scope of PR code, recorded here only as context observed in commit history.
- No code/config files changed by this step; `ast-index rebuild` not required (markdown step report only).
