Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~65 minutes wall-clock (session started reading `agents/ALL.md`/task folder ~16:00 local, this report written ~16:24 UTC same session)
Tokens used: not precisely instrumented from inside this agent
Changed files: see full lists in §1/§2 below (2 new source files, 1 new test file already counted, 16 modified files, plus this step report)

---

# Coding: implement `003-architecturing.md`'s duplicate-key-to-409 convention (email + users + admin)

## 0. Scope and method

Implemented `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/003-architecturing.md` exactly as specified: every file's "before" state was read and confirmed to match the spec's assumed baseline before editing (no drift), then each file was brought to the spec's exact "after" content (KDoc text, code diffs, README prose all adopted verbatim from the spec). No design decisions were made in this step — the architecture was already fully resolved and marked READY.

## 1. New files (2)

1. `features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt` — the new exception type, package `dev.inmo.wishlist.features.users.common.repo.exceptions`, adopted verbatim from spec §1.1.
2. `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` — first-ever `jvmTest` source file in `features/users/common` (the module is genuinely multiplatform, so this JVM-only `java.sql.SQLException` test could not live in `commonTest`), adopted verbatim from spec §2.1. Three tests: Postgres unique-violation SQL state (`23505`) → `true`; a different SQL state (`23503`) → `false`; null SQL state → `false`.

## 2. Modified files (16)

**Users module:**
1. `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` — `emailColumn` gained `.uniqueIndex()` (the literal PR review-comment fix); `update`/`create` are now overridden to catch `ExposedSQLException`, check `isUniqueViolation()` (new file-scope `internal fun SQLException.isUniqueViolation(): Boolean = sqlState == "23505"`), and translate a match into `DuplicateUserFieldException` while rethrowing everything else unchanged; KDoc updated throughout.
2. `features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt` — KDoc-only addition (interface previously had none).
3. `features/users/common/src/commonMain/kotlin/repo/CacheUsersRepo.kt` — KDoc-only additions (class + `getUserByUsername` override); confirmed (per architecture spec's own verification) zero code change needed since `FullCRUDCacheRepo`'s write wrapper does not catch exceptions from the wrapped repo.

**Email server module:**
4. `features/email/server/src/commonMain/kotlin/EmailFeature.kt` — `@throws` KDoc added to `setMyEmail`.
5. `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — same `@throws` KDoc added to `setMyEmail`.
6. `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt` — same `@throws` KDoc added to `setMyEmail`.
7. `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt` — same `@throws` KDoc added to `updateStoredEmail`.
8. `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` — new import `DuplicateUserFieldException`; class KDoc updated; `put(EmailConstants.myEmailPathPart)` handler now wraps `feature.setMyEmail(...)` in `try`/`catch (e: DuplicateUserFieldException)` and responds `409 Conflict` before the existing `when { updated -> OK; else -> 500 }` block.

**Admin server module:**
9. `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — `@throws` KDoc added to `create` and `update` (neither had any KDoc before).
10. `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — new import `DuplicateUserFieldException`; class KDoc updated (both the users-routes bullet list and a new closing note); `post(Constants.usersCreatePathPart)` and `put("${Constants.usersUpdatePathPart}/{id}")` handlers each wrap their `adminFeature.usersManagement.*` call in a `try`/`catch (e: DuplicateUserFieldException)` responding `409 Conflict`.

**Tests (email server `commonTest`):**
11. `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` — `updateObject`/`createObject` now enforce the same uniqueness rule `ExposedUsersRepo` enforces at the DB level (throwing `DuplicateUserFieldException` on a colliding username or non-null email, excluding self). **Deviates from the architecture spec's literal text in one respect, found and fixed during the build-verification cycle — see §4 below**: the uniqueness checks read the inherited `protected val map` field directly instead of calling `getAll()`.
12. `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` — new test `setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken`, adopted verbatim from spec §2.3.
13. `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt` — new test `setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken` (mirrors §2.3 against `DisabledEmailFeature`), adopted verbatim from spec §2.4.

**READMEs:**
14. `features/users/README.md` — Models table gained a `DuplicateUserFieldException` row; "Email field" Architecture Notes bullet updated to mention `.uniqueIndex()`; new "Duplicate-key-to-409 convention" Architecture Notes bullet added. `## Operator Notes` untouched (this feature's README has an empty Operator Notes section; left as-is).
15. `features/email/README.md` — Routes table's `PUT /email/myEmail` row updated (`200 OK` or `409 Conflict`); new "Duplicate email → 409" Architecture Notes bullet added after "Root guard". `## Operator Notes` untouched.
16. `features/admin/README.md` — Routes table's `POST /admin/users/create` and `PUT /admin/users/update/{id}` rows updated with `409`; new "Duplicate username/email → 409" Architecture Notes bullet added after the `AdminRoutingsConfigurator` bullet. `## Operator Notes` (the human-authored constraints at the top) left completely untouched.

All text/code above was adopted verbatim from `003-architecturing.md` unless noted in §4.

## 3. `ast-index rebuild`

Run twice: once after the initial full set of `.kt` edits, and once more after the one fix-cycle change to `FakeUsersRepo.kt` (§4). Both runs completed cleanly (681 files indexed, 46 modules, no errors).

## 4. One fix cycle — deadlock in `FakeUsersRepo`'s new uniqueness check

Per `agents/CODING.md`'s "do only one cycle of fixing" rule, this was the one cycle:

**Symptom** (reported by the coordinator, who ran the three-module build directly): `wishlist.features.users.common:build` and `wishlist.features.admin.server:build` passed; `wishlist.features.email.server:allTests` failed 5 of 30 tests, all with `kotlinx.coroutines.test.UncompletedCoroutinesError` — exactly the 5 tests that exercise `EmailFeatureService.setMyEmail`/`DisabledEmailFeature.setMyEmail`'s success path through `usersRepo.update(...)` (both new duplicate-propagation tests, plus 3 pre-existing tests that were passing before this task's changes).

**Root cause** (read `MicroUtils`' `MapCRUDRepo.kt`/`SmartRWLocker.kt` sources directly, local checkout at `/home/aleksey/projects/own/MicroUtils`): `WriteMapCRUDRepo.update()`/`.create()` call `updateObject`/`createObject` from *inside* `locker.withWriteLock { }`. The architecture spec's literal `FakeUsersRepo` body called `getAll()` inside those overrides to check for collisions — but `getAll()` (`ReadMapCRUDRepo.getAll()`) itself calls `locker.withReadAcquire { }`, which first does `_writeMutex.waitUnlock()`. Since the *same* `SmartRWLocker` instance backs both the read and write delegates in `MapCRUDRepo`, and the write mutex is non-reentrant, this is a self-deadlock: the write-lock holder blocks forever waiting for a write-unlock that only it can perform. `runTest` surfaces a coroutine stuck forever as `UncompletedCoroutinesError`. This bug did not exist before this task because the pre-existing `updateObject`/`createObject` never called `getAll()`.

**Fix**: in `FakeUsersRepo.kt`, both `updateObject` and `createObject` now read the inherited `protected val map: MutableMap<UserId, RegisteredUser>` (from `WriteMapCRUDRepo`, accessible to this subclass) directly via `map.values.any { ... }` instead of `getAll().values.any { ... }`. This is safe and correct because these methods only ever run while the write lock is already held (no concurrent writer can be mutating `map`), and KDoc was added on both methods explaining exactly why `getAll()` must not be used here (deadlock risk). No other file needed a change for this fix — the deadlock was entirely local to the test double.

**Re-verification**: ran `./gradlew :wishlist.features.email.server:build` in the foreground to completion after the fix: `BUILD SUCCESSFUL`, all test-result XML files under `features/email/server/build/test-results/jvmTest/` show `tests="4|2|7|3|3|3|8"` summing to 30, `failures="0" errors="0"` across all of them.

## 5. Build results (final, all three modules re-verified in the foreground after the fix)

- `./gradlew :wishlist.features.users.common:build` — **PASS** (`BUILD SUCCESSFUL`; `IsUniqueViolationTest`'s 3 tests present in `build/test-results/jvmTest/TEST-....IsUniqueViolationTest.xml`, `tests="3" failures="0" errors="0"`).
- `./gradlew :wishlist.features.email.server:build` — **PASS** (`BUILD SUCCESSFUL`; 30/30 tests green after the fix in §4).
- `./gradlew :wishlist.features.admin.server:build` — **PASS** (`BUILD SUCCESSFUL`; `jvmTest`/`allTests` are `NO-SOURCE` — this module has no test files at all, matching `003-architecturing.md` §3's explicitly-flagged, accepted testing-infrastructure gap, not a new regression).

No full-project (`./gradlew build`) run was performed — the task's instructions scoped verification to the three directly-affected modules, and the coordinator's follow-up only asked for these three plus the fix-cycle re-run.

## 6. Explicitly not touched

- `## Operator Notes` sections in all three touched READMEs — left byte-for-byte unchanged.
- `agents/security-review-2026-07-09.md` and `agents/task/06-07-2026_07-28-28-52286c3b-14d7-437a-82e7-cc39850f732d/`, `agents/task/24-06-2026_15-10-08-b1b6b605-82ca-4f2a-bebe-95bedd5ee0e5/` (other task folders) — untouched, not staged.
- `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/PROMPT.md` — pre-existing untracked file in this task's own folder, not created or modified by this step; left for a separate chore-style commit (matching this repo's existing git-log convention of dedicated `chore: record task PROMPT.md` commits), not bundled into this Coding commit.

## 7. Status

**Build: PASS across all three modules.** Ready for Verification.
