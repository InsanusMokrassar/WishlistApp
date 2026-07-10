Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: not tracked by harness; single continuous implementation+build+test pass
Tokens used: not tracked by harness
Changed files: `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt`, `features/admin/server/src/commonMain/kotlin/Plugin.kt`, `features/admin/server/build.gradle`, `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`, `features/email/server/src/commonMain/kotlin/Plugin.kt`, `features/email/server/build.gradle`, `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` (re-pointed), `features/email/server/src/commonTest/kotlin/services/FakeSimpleRolesFeature.kt` (new), `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt`, `features/files/server/src/commonMain/kotlin/Plugin.kt`, `features/files/server/build.gradle`, `features/ui/users/src/commonMain/kotlin/Plugin.kt`, `features/ui/users/build.gradle`, `features/admin/README.md`, `features/email/README.md`, `features/files/README.md`, `features/ui/users/README.md`, `features/users/README.md`, `features/auth/README.md`, this report

# Coding pass 4/4: Issue #68 — Call-site replacement (security-sensitive), final pass

**Scope note:** this is Coding **pass 4 of 4** for `agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/003-architecturing.md`'s 4-pass split (§4 "Pass 4 — Call-site replacement"). Pass 1 (Foundation), Pass 2 (Bootstrap + migration), and Pass 3 (Aggregator + guard) were already committed (`a733130`/`2dfdf46`/`53cfc4b`) before this step started. This pass is the last Coding pass for issue #68 — the cycle now moves to Verification.

## 1. What this pass delivered

Byte-exact against `003-architecturing.md` §6 (before/after diffs) and §8.8/§8.9 (test code), all 19 files from the spec's Pass-4 file list (§9), no more, no fewer (confirmed via `git status` after all edits: 18 modified + 1 new = 19).

### 1.1 Server call site 1 — `features/admin/server/.../AdminRoutingsConfigurator.kt`

- Constructor gained `simpleRolesFeature: SimpleRolesFeature`; the class-level `rootUsername` field and `usersRepo`-lookup were removed from `requireAdmin()`, replaced by `if (!simpleRolesFeature.isSuperAdmin(callerId))`. `usersRepo: ReadUsersRepo` itself stays on the constructor — still used by the unrelated `GET /admin/users/getById/{id}` route.
- Class KDoc updated: "the authenticated caller must be the `root` user" → "the authenticated caller must hold the SuperAdmin role"; "Non-root callers" → "Non-SuperAdmin callers".
- `Plugin.kt`: `AdminRoutingsConfigurator(...)` construction gained `simpleRolesFeature = get<SimpleRolesFeature>()`.
- `build.gradle`: added `api project(":wishlist.features.simpleRoles.server")`.

### 1.2 Server call site 2 — `features/email/server/.../EmailFeatureService.kt`

- Constructor gained `simpleRolesFeature: SimpleRolesFeature`; `rootUsername` field removed; `sendTestEmail` now does `if (!simpleRolesFeature.isSuperAdmin(callerId)) return false` — the previous `usersRepo.getById(callerId) ?: return false` lookup is gone (an unknown `UserId` now resolves to `isSuperAdmin == false` directly, same net "false" outcome, one fewer round trip). `setMyEmail` unaffected — still delegates to `updateStoredEmail(usersRepo, ...)`.
- Class/method KDocs updated to describe the SuperAdmin check instead of the root-username comparison.
- `Plugin.kt`: `EmailFeatureService(it, get<UsersRepo>())` → `EmailFeatureService(it, get<UsersRepo>(), get<SimpleRolesFeature>())`.
- `build.gradle`: added `api project(":wishlist.features.simpleRoles.server")`.
- Tests re-pointed: new `commonTest/services/FakeSimpleRolesFeature.kt` (fixed-answer `SimpleRolesFeature` double, records every checked `UserId` in `calls`), full re-point of `EmailFeatureServiceTest.kt` (byte-exact against spec §8.9) — 7 tests: `isFeatureEnabledAlwaysReturnsTrue`, `sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsTrueResult`, `sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsFalseResult`, `sendTestEmailReturnsFalseWhenCallerIsNotSuperAdminAndDoesNotCallSendText`, `setMyEmailPersistsViaUsersRepoForFoundUser`, `setMyEmailReturnsFalseWhenUserNotFound`, `setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken`. The old "caller not found in `usersRepo`" test case is removed as a distinct case (folded into "not superadmin", per spec's own reasoning) — non-root-caller and caller-not-found tests are unified into one `sendTestEmailReturnsFalseWhenCallerIsNotSuperAdminAndDoesNotCallSendText` test using `UserId(999L)`.

### 1.3 Server call site 3 — `features/files/server/.../FilesRoutingsConfigurator.kt`

- Constructor: `usersRepo: ReadUsersRepo` **replaced** (not added alongside) by `simpleRolesFeature: SimpleRolesFeature` — `ReadUsersRepo` had no other use in this class (confirmed by re-reading the full file). The private `rootUsername` field and `isRoot(callerId)` helper are removed entirely; the avatar-`PUT` guard now reads `if (callerId != userId && !simpleRolesFeature.isSuperAdmin(callerId))`.
- Import swapped: `dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo` → `dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature`. `UserId` import stays (still used by two `?.let(::UserId)` calls).
- Class KDoc's `@param usersRepo` line replaced by `@param simpleRolesFeature`.
- `Plugin.kt`: `FilesRoutingsConfigurator(get(), get())` → `FilesRoutingsConfigurator(get(), get<SimpleRolesFeature>())`.
- `build.gradle`: added `api project(":wishlist.features.simpleRoles.server")`.

### 1.4 Client call site — `features/ui/users/.../Plugin.kt`

- `UsersModel.isCurrentUserRootFlow` changed from `meState.map { it?.username?.string == "root" }.stateIn(...)` to `get<CacheSimpleRolesFeature>().isSuperAdminStateFlow` — property name and `StateFlow<Boolean>` type unchanged, only the source mechanism changed (per spec §2.2's decision that SuperAdmin stays architecturally fixed to `root`).
- Added import `dev.inmo.wishlist.features.simpleRoles.client.CacheSimpleRolesFeature`.
- Verified `scope`/`stateIn`/`SharingStarted` stay in use elsewhere in the same `single<UsersModel> { }` block (`currentUserIdFlow`) — no import went dead.
- **Compile-scoping check (deviation risk assessed, none needed):** the spec's after-code calls `get<CacheSimpleRolesFeature>()` directly inside the anonymous `object : UsersModel { }` body, unlike every other Koin resolution in the same block (`feature`, `adminFeature`, `filesService`, `scope`, `credentialsStorage`, all captured as local `val`s *before* the object expression). This looked like a possible Kotlin implicit-receiver-scoping issue worth verifying rather than assuming. Verified empirically: `:wishlist.features.ui.users:build` (jsMain/jvmMain/androidMain all compile) succeeded with the spec's exact code, no local-`val` capture needed — implemented byte-exact per spec, no deviation.
- **Downstream consumers confirmed unchanged, per spec §6.4 and directly re-verified this step:** `UserViewModel.canEditState`, `UserEditViewModel.isRootState`/`canSaveState`, and the three platform `UserEditView.kt`s (js/jvm/android) all read `UsersModel.isCurrentUserRootFlow` (or `UserEditViewModel.isRootState`, itself `= model.isCurrentUserRootFlow`) unchanged — they transitively pick up the new SuperAdmin-backed behavior with **zero code changes**, exactly as the spec states. `UsersModel.kt` itself also needs no code change (only the interface declaration, unchanged in shape). Left their prose KDocs ("the caller is `root`" / "only identity permitted...") untouched — spec explicitly marks this as optional, non-blocking prose-only guidance, and touching them risked scope creep beyond the exact spec diffs on the most security-sensitive pass of the task.
- `build.gradle`: added `api project(":wishlist.features.simpleRoles.client")`.

### 1.5 README updates (6 files, per spec §7)

- `features/admin/README.md` — `AdminRoutingsConfigurator` Architecture Notes bullet reworded to describe the SuperAdmin check via `simpleRoles.server`.
- `features/email/README.md` — `EmailFeatureService` Models-table row and the "Root guard" → "Superadmin guard" Architecture Notes bullet reworded (byte-exact per spec §7); additionally updated one stale code-snippet reference in the "DI placement" bullet (`EmailFeatureService(it, get())` → `EmailFeatureService(it, get(), get())`) to keep it accurate — not explicitly called out in spec §7 but in the same file/section already being edited, so folded in per `agents/CODING.md`'s "update KDocs/README to match" rule.
- `features/files/README.md` — Routes-table `PUT /files/avatar/{userId}` row and the "User avatars" Architecture Notes bullet reworded (byte-exact per spec §7).
- `features/ui/users/README.md` — Overview's 3 "and `root`" / "`root` may" phrasings replaced with "and a SuperAdmin" / "a SuperAdmin may" (2 bullets, profile-view + profile-edit); Models table's `UsersModel` row parenthetical updated; "Root detection is client-side" bullet replaced with the "Superadmin detection is client-side" bullet (byte-exact per spec §7).
- `features/users/README.md` — optional one-line addition to the "Root user bootstrap" Architecture Notes bullet, pointing to `roles/README.md`.
- `features/auth/README.md` — optional one-line addition to Architecture Notes (appended after the `purgeUser` bullet, since this file has no literal "Root-user bootstrap" line to anchor after — closest existing bootstrap-adjacent context used instead), pointing to `roles/README.md`.
- No `## Operator Notes` section was touched in any of the 6 files.

## 2. Repo-wide grep confirmation (task's explicit verification requirement)

Ran after all edits, across `features/**/*.kt`:

```
grep -rn "rootUsername\|== \"root\"\|!= \"root\"\|\.string == .root.\|\.string != .root." features/ --include="*.kt"
```

Remaining hits, all legitimate and none are privilege checks:

- `features/auth/server/src/jvmMain/kotlin/JVMPlugin.kt:20,44,49` — the **pre-existing, unrelated** root-*user-creation* bootstrap (`agents/CODING.md`'s "Root-user bootstrap" section): on first startup, if `UsersRepo.count() == 0`, creates a user literally named `"root"` and prints its generated password. This is account **creation**, not a privilege **check** — no code path here compares an existing caller's username against `"root"` to decide access. It predates issue #68 entirely and is explicitly out of this pass's (and the whole issue's) scope — the architecture spec never lists this file anywhere across all 4 passes.
- `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt:11,28` — Pass 2's bootstrap/migration, exactly as expected: `if (user.username.string == rootUsername)` decides who additionally gets the `SuperAdmin` role grant on top of the `User` role every account gets. This is the one sanctioned place, per the task brief, where the literal `"root"` username may still be used ("to determine who gets the SuperAdmin role at bootstrap").
- `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt` and `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt` — test fixtures naming a `RegisteredUser` `"root"`/`rootUser` for readability; `DisabledEmailFeatureTest`'s `rootUser` fixture is inert (`DisabledEmailFeature.sendTestEmail` unconditionally returns `false`, no username comparison at all) — not privilege-check code paths, out of scope.

**Confirmed: zero privilege-check code paths anywhere in the app still hardcode the literal `"root"` username comparison after this pass.** A supplementary check (`grep -rln "isRoot(\|isCurrentUserRoot\b" features/ --include="*.kt" | grep -v isCurrentUserRootFlow` and `grep -rln "\.username\.string ==" features/ --include="*.kt"`) returned no hits outside `RolesBootstrap.kt`, confirming no stray root-comparison helper (like the old `isRoot()`/`requireAdmin()` inline checks) survived anywhere.

## 3. Build and test result

Ran in the foreground, waited for completion (not backgrounded):

1. `ast-index rebuild` — 728 files, 52 modules indexed, clean.
2. Scoped build first: `./gradlew :wishlist.features.admin.server:build :wishlist.features.email.server:build :wishlist.features.files.server:build :wishlist.features.ui.users:build` — **BUILD SUCCESSFUL** in 46s, 1252 actionable tasks (89 executed, 1163 up-to-date). `EmailFeatureServiceTest` confirmed green: `features/email/server/build/test-results/jvmTest/TEST-...EmailFeatureServiceTest.xml` — `tests="7" failures="0" errors="0"`.
3. Full project build: `./gradlew build` — **BUILD SUCCESSFUL** in 1m 53s, 4339 actionable tasks (261 executed, 4078 up-to-date). Scanned all `TEST-*.xml` files under every module's `build/test-results/` for `failures="[1-9]`/`errors="[1-9]`: **36 total XML files, zero failures/errors** — same count as Pass 3's report (this pass re-pointed an existing test file and added one non-test fixture, so the file-count itself was not expected to change).

No build-fix cycle was needed — the spec's exact code compiled cleanly the first time, including the `get<CacheSimpleRolesFeature>()` call inside the `ui/users` anonymous object (see §1.4).

## 4. Manual verification (spec's Pass 4 "Verify" section)

Not performed this step — running the live server + curling `/admin/...`, `PUT /files/avatar/{userId}`, and `POST /email/sendTest` with both a non-superadmin and the `root` bearer token, plus a client-side check that a non-superadmin logged-in user cannot see root-only edit affordances in `features/ui/users`, requires booting the app end-to-end (DB, server process, client). Per this task's own instructions, the gate for this Coding pass is the full build + `EmailFeatureServiceTest` green (both achieved, §3); the manual runtime check is left to Verification, which is the next stage of this cycle.

## 5. Deviations from the spec

None. All 19 files match `003-architecturing.md` §6/§7/§8.8/§8.9 byte-exact (or, for the two "optional" README lines in `features/users/README.md`/`features/auth/README.md`, applied per the spec's own stated optional guidance, since no literal anchor line existed to append after in `auth/README.md` — used the nearest relevant Architecture Notes bullet instead, as the spec itself allowed "Overview or Architecture Notes" flexibility for that one). The one additional edit beyond the spec's literal §7 text — the `features/email/README.md` "DI placement" bullet's stale `EmailFeatureService(it, get())` code snippet — was made for KDoc/README accuracy per `agents/CODING.md`, in a file already being edited for this pass; no other unlisted files were touched.

## 6. Status of issue #68 overall

All 4 passes (Foundation, Bootstrap+migration, Aggregator+guard, Call-site replacement) are now implemented, tested, and building green. Every server- and client-side root-privilege check in the app (issue #68 points 8 and 9) now goes through `SimpleRolesFeature.isSuperAdmin`, itself backed by the `kroles`-based `features/roles` role graph seeded by Pass 2's bootstrap/migration. This is the final Coding pass for this task; next stage is Verification.
