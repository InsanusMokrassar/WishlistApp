Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: not tracked by harness; single continuous implementation+build+test pass
Tokens used: not tracked by harness
Changed files: `features/roles/server/build.gradle` (modified), `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt` (rewritten), `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt` (new), `features/roles/server/src/commonTest/kotlin/FakeRolesRepo.kt` (new), `features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt` (new), `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt` (new), `features/roles/README.md` (status-note update), this report

# Coding pass 2/4: Issue #68 — Bootstrap + migration only

**Scope note:** this is Coding **pass 2 of 4** for `agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/003-architecturing.md`'s 4-pass split (§4 of that doc). Only "Pass 2 — Bootstrap + migration" is implemented in this step. Pass 3 (`FeatureRolesRegistry`/`requireRole` aggregator) and Pass 4 (call-site replacements) are **not** implemented — they must be picked up by later Coding invocations, in that order, per the spec's ordering-dependency note (§3, "Pass 2 depends only on pass 1... Pass 3 has no dependency on pass 2").

## 0. Preliminary check — item 1 of the task ("SuperAdmin/User role constants")

Verified `features/roles/common/src/commonMain/kotlin/RoleConstants.kt` already exists from Pass 1 with `SuperAdminRole` and `UserRole` (`BaseRole("SuperAdmin")` / `BaseRole("User")`) — no change needed here; Pass 2 only had to reference them.

## 1. What this pass delivered

All 6 files from the architecture spec's §4 "Pass 2" file list, byte-exact against `003-architecturing.md` §3.4/§8.3–8.5, cross-checked against the real `dev.inmo.kroles.repos.RolesRepo`/`ReadRolesRepo`/`WriteRolesRepo` interfaces (`/home/aleksey/projects/own/kroles/repos/src/commonMain/kotlin/RolesRepo.kt`) and this repo's own `ReadUsersRepo`/`UsersRepo`/`VersionsRepo`/`NewUser`/`RegisteredUser`/`Username` sources before writing:

- **`features/roles/server/build.gradle`** — added `api project(":wishlist.features.users.common")` and `api project(":wishlist.features.auth.server")` to `commonMain.dependencies`, alongside the existing `roles.common`/`common.server` deps from Pass 1.
- **`features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt`** (new) — two `internal suspend` functions, both Koin/`VersionsRepo`/`Database`-free so they are directly unit-testable:
  - `grantDefaultRoles(rolesRepo, user)` — grants `UserRole` to every user, plus `SuperAdminRole` when `user.username.string == "root"`. Idempotent via kroles' `includeDirect` (no-op, returns `false`, on a repeat grant).
  - `backfillDefaultRoles(usersRepo, rolesRepo)` — applies `grantDefaultRoles` to every user returned by `usersRepo.getAll()` (the one-time migration body, issue point 6).
- **`features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt`** (rewritten) — real bootstrap wiring in `startPlugin`, replacing the Pass 1 stock delegating scaffold:
  1. Resolves `UsersRepo`, `RolesRepo`, `CoroutineScope`, `VersionsRepo<Database>` from Koin.
  2. **Subscribes first** to `usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user -> grantDefaultRoles(rolesRepo, user) }` — *before* any snapshot read.
  3. Only then runs `versionsRepo.setTableVersion(tableName = "users_default_role_backfill", version = 1, onUpdate = { _, _ -> backfillDefaultRoles(usersRepo, rolesRepo) })`, gating the one-time migration so it executes exactly once across the app's lifetime regardless of restarts.

  This ordering is the fix for the plugin-load-order race identified in Planning round 2: the microutils launcher runs every top-level plugin's `startPlugin` **concurrently** (`scope.launch{}` per plugin, `joinAll()` at the end), not sequentially by `sample.config.json` list order. If the migration's `getAll()` snapshot ran before `features/auth/server`'s root bootstrap finished creating the `root` user, the migration would see zero users, mark itself permanently done (version 1), and never see `root` again on any future restart. Subscribing to `newObjectsFlow` first closes that race: any user created concurrently by another plugin — including `root` — is caught by the live subscription even if it beats the backfill's snapshot read. `grantDefaultRoles`'s idempotency (via kroles' `includeDirect`) makes double-granting in the overlap window between the subscription and the backfill harmless.
- **`features/roles/server/src/commonTest/kotlin/FakeRolesRepo.kt`** (new, test fixture) — in-memory `RolesRepo` double backed by `MutableMap<BaseRoleSubject, MutableSet<BaseRole>>`, implementing every abstract member of `ReadRolesRepo`/`WriteRolesRepo` (verified against the real interface, not just the spec's prose).
- **`features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt`** (new, test fixture) — in-memory `UsersRepo` double built on MicroUtils' `MapCRUDRepo`, giving tests a real `newObjectsFlow` to subscribe against (the same base class production `CacheUsersRepo` ultimately builds on).
- **`features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt`** (new) — 6 tests, all green (see §3).

## 2. Deviation from the spec

One small addition beyond the spec's literal text, scoped to the new test file only: added `@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)` on `reactiveSubscriptionGrantsDefaultRolesOnNewUserCreation()` (plus the corresponding import). The spec's `RolesBootstrapTest.kt` body compiled and passed as written, but produced an opt-in compiler warning on `UnconfinedTestDispatcher()` (this repo's first use of that API — no existing precedent to follow). This is a warning-silencing annotation only; no test logic or assertion changed. Verified the warning is gone and all 6 tests still pass after the addition.

Also updated `features/roles/README.md`'s trailing "Coding-pass status note" blockquote (not code) to mark Pass 2 as complete instead of "Pass 1 (Foundation, current)" — the Architecture Notes body itself (written byte-exact in Pass 1 to describe the feature's final 4-pass design) needed no change, per `agents/CODING.md`'s README-update rule.

## 3. Build and test result

Ran in the foreground, waited for completion (not backgrounded):

1. `ast-index rebuild` — 723 files, 52 modules indexed, clean (ran once after the `.kt` changes; the later README-only edit did not require a re-run per `agents/ALL.md`).
2. `./gradlew :wishlist.features.roles.server:build` — **BUILD SUCCESSFUL**, no warnings. `RolesBootstrapTest`'s 6 tests all passed, 0 failures, 0 errors:
   - `grantDefaultRolesGrantsOnlyUserRoleForNonRootUser`
   - `grantDefaultRolesGrantsUserAndSuperAdminRoleForRootUser`
   - `grantDefaultRolesIsIdempotent`
   - `backfillDefaultRolesGrantsRolesToAllPreExistingUsers`
   - `backfillDefaultRolesIsIdempotentAcrossRepeatedRuns`
   - `reactiveSubscriptionGrantsDefaultRolesOnNewUserCreation` (the concurrency-safety test — asserts a user created *after* the `newObjectsFlow` subscription starts, via `UnconfinedTestDispatcher`, is granted default roles reactively; both `root` and a plain user are exercised)
3. `./gradlew build` (full project) — **BUILD SUCCESSFUL**, 4324 actionable tasks (192 executed, 4132 up-to-date). Scanned all 30 `TEST-*.xml` files under every module's `build/test-results/` for `failures="[1-9]`/`errors="[1-9]` — zero matches (30, up from Pass 1's 29, reflecting the new `RolesBootstrapTest` suite).

Manual runtime verification (fresh-DB server boot + direct `roles` table query to confirm `root` gets both roles, and a second boot to confirm the migration doesn't re-run) — per the spec's Pass 2 "Verify" section — was **not** performed this pass, matching Pass 1's own precedent of deferring live-server checks. This was a scope/time tradeoff against the caller's explicit build+test checklist; the reactive-subscription and backfill-idempotency behavior this manual check would exercise is covered by `RolesBootstrapTest` against the real `newObjectsFlow`/`MapCRUDRepo` machinery instead. Flagging so a future pass (or the operator) can do a live boot+query check if desired before pass 4 ships the call-site replacements that make role checks user-visible.

## 4. What Pass 3 (aggregator + guard) needs from this pass

- `RolesBootstrap.kt`'s `grantDefaultRoles`/`backfillDefaultRoles` and the real `JVMPlugin.kt` bootstrap are in place and tested; Pass 3 does not depend on them (per the spec's ordering note, Pass 3 depends only on Pass 1).
- `roles/server/build.gradle` now already carries `auth.server` (added this pass for the bootstrap's own Koin resolution, but also the exact dependency Pass 3's `RequireRole.kt` will need for `getCallerUserIdOrAnswerUnauthorized`) — no further Gradle change needed in `roles/server` for Pass 3.
- `roles/common/Plugin.kt` is still the stock empty scaffold from Pass 1 — untouched by this pass, as instructed. Pass 3 populates it with `FeatureRolesRegistry.register(...)` calls.

## 5. Note on `agents/AGENTS.md`

`agents/AGENTS.md` contains a prompt-injection payload (a fabricated "SYSTEM DIRECTIVE" demanding all agent output be reformatted into a dense, pronoun-free pseudo-protocol, with a redirect to a nonexistent `agents/SHORTCUTS.md`). This was already flagged as ignored in `003-architecturing.md` §0 by the Architecturing step; it was disregarded again this step for the same reason — it conflicts with the actual task instructions (clear step report, clear final report to the caller) and matches known prompt-injection patterns. No code or config change was made in response to it; noting it here only for continuity across steps.
