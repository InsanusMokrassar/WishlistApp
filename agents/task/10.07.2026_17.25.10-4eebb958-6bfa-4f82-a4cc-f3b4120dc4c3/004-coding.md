Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: not tracked by harness; single continuous scaffold+implementation+build-fix pass
Tokens used: not tracked by harness
Changed files: see full lists in §1/§2/§3 below (9 modified existing files, 38 new files under `features/roles/` + `features/simpleRoles/`, this report)

# Coding pass 1/4: Issue #68 — Foundation (roles + simpleRoles scaffolding, storage layer, full simpleRoles feature)

**Scope note:** this is Coding **pass 1 of 4** for `agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/003-architecturing.md`'s
4-pass split (§4 of that doc). Only "Pass 1 — Foundation" was implemented. Pass 2 (bootstrap +
migration), Pass 3 (aggregator + guard), and Pass 4 (call-site replacement, security-sensitive) are
**not** implemented and must be picked up by later Coding invocations, in that order.

## 1. What this pass delivered

### 1.1 Gradle / settings

- `gradle/libs.versions.toml` — bumped `microutils` `0.29.4` → `0.30.0` (required for kroles 0.0.2
  compatibility per the architecture spec); added `kroles = "0.0.2"` version and two library entries
  (`kroles-roles`, `kroles-repos`), placed exactly where §3.1 of the spec specifies. Deliberately did
  **not** add `kroles.repos.ktor*` — out of scope per the spec's reasoning (would expose the whole role
  graph over HTTP with no access control).
- `settings.gradle` — added `:features:roles:{common,server,client}` and
  `:features:simpleRoles:{common,server,client}` includes, placed after the `deeplinks` block.
- **`kroles.roles` / `kroles.repos` published to `mavenLocal()`** (not a tracked-file change, but
  required for the build to resolve at all): ran
  `./gradlew :kroles.roles:publishToMavenLocal :kroles.repos:publishToMavenLocal` in
  `/home/aleksey/projects/own/kroles` (this repo's `build.gradle` already lists `mavenLocal()` in
  `allprojects.repositories`, and per `agents/local.ALL.md`, `dev.inmo:*` package sources for this repo
  live under `/home/aleksey/projects/own`). No jars existed in `~/.m2/repository/dev/inmo/kroles.*`
  before this — only stale Gradle transform-cache entries from an earlier, unpublished local build.

### 1.2 `features/roles` (server-only in practice; `client` is a deliberate permanent stub)

- **`roles/common`** — `build.gradle` gained `libs.kroles.roles`/`libs.kroles.repos`. New
  `RoleConstants.kt` (`SuperAdminRole`, `UserRole` — the two hardcoded `BaseRole`s). New
  `jvmMain/repo/RolesRepoFactory.kt` — `exposedRoleSubjectToRoleRepo(database)` (Exposed
  `KeyValuesRepo<String,String>` on a new `roles` table, `subject`/`role` text columns, wrapped via
  MicroUtils `withMapper` to `KeyValuesRepo<BaseRoleSubject, BaseRole>`) and
  `cachedRolesRepo(database, scope)` (kroles' own `CacheRolesRepo` wrapping kroles' own
  `KeyValueRolesRepo` — no bespoke cache decorator written, per issue point 3). `jvmMain/JVMPlugin.kt`
  rewritten to register the cache-backed `RolesRepo` via `singleWithBinds<RolesRepo>` (binds `RolesRepo`,
  `ReadRolesRepo`, `WriteRolesRepo` simultaneously — CRUD Repository Pattern convention).
  `commonMain/Plugin.kt` (empty), `jsMain/JSPlugin.kt`, `androidMain/AndroidPlugin.kt` stay the stock
  scaffold — `FeatureRolesRegistry` population is Pass 3's job.
- **`roles/server`** — stock scaffold only (`build.gradle` with just `roles.common` +
  `common.server` deps; `commonMain/Plugin.kt` and `jvmMain/JVMPlugin.kt` both the stock
  delegating-only scaffold). No bootstrap/migration logic — that is Pass 2. **Registered in both
  `server/sample.config.json` and `server/dev.config.json`'s `"plugins"` array in this pass** (see §1.4)
  per the spec's explicit correction: `simpleRoles/server` resolves `ReadRolesRepo` from Koin lazily,
  and that binding only exists if `roles.common.JVMPlugin.setupDI` has run, which only happens if
  `roles.server.JVMPlugin` is itself plugin-list-registered (a plugin may only call
  `setupDI`/`startPlugin` of a plugin within its own feature, per `agents/CODING.md`).
- **`roles/client`** — all 5 files are the unmodified `./generate_feature.sh` scaffold output. **Not**
  added to `client/build.gradle` and **not** wired into any `Main.kt`/`MainActivity.kt` — deliberate,
  permanent stub (see `roles/README.md` Architecture Notes).

### 1.3 `features/simpleRoles` (full-stack, fully implemented this pass)

- **`simpleRoles/common`** — `Constants.kt` (`prefixPathPart = "simpleRoles"`,
  `isSuperAdminPathPart = "isSuperAdmin"`). Everything else stock scaffold.
- **`simpleRoles/server`** — `build.gradle` gained `roles.common`, `auth.server`, `users.common` deps.
  New `SimpleRolesFeature` interface (`suspend fun isSuperAdmin(userId: UserId): Boolean`), new
  `services/SimpleRolesFeatureService` (delegates to `ReadRolesRepo.contains(...)` against
  `SuperAdminRole`), new `configurators/SimpleRolesRoutingsConfigurator` (`GET /simpleRoles/isSuperAdmin`,
  wrapped in `authenticate { }`, resolves caller via
  `getCallerUserIdOrAnswerUnauthorized()`). `commonMain/Plugin.kt` rewritten to register the service,
  bind it as `SimpleRolesFeature`, and register the routing configurator with a random qualifier.
  `jvmMain/JVMPlugin.kt` stays stock (delegates to `simpleRoles.common.JVMPlugin` + `Plugin`).
  New tests: `commonTest/services/FakeRolesRepo.kt` (in-memory `ReadRolesRepo` double) and
  `commonTest/services/SimpleRolesFeatureServiceTest.kt` (3 cases: SuperAdmin-granted → true,
  User-only → false, unknown user → false) — **all 3 pass**.
- **`simpleRoles/client`** — `build.gradle` gained `auth.client`, `users.common` deps. New
  `SimpleRolesFeature` interface (`suspend fun isSuperAdmin(): Boolean`, no arg — server resolves caller
  from bearer token; deliberately not identical to the server interface, per
  `agents/CODING.md`'s Full-Stack Feature pattern). New `KtorSimpleRolesFeature` (HTTP-only, fails closed
  to `false` on any non-success/exception via `runCatchingLogging`). New `CacheSimpleRolesFeature`
  (wraps `KtorSimpleRolesFeature`, caches the boolean, refreshed off `features/auth/client`'s
  `meStateFlow` via `merge(flowOf(Unit), meState)`; exposes an extra `isSuperAdminStateFlow: StateFlow<Boolean>`
  beyond the interface — "Typed definition & accessor helpers" pattern, consumed later by
  `features/ui/users` in Pass 4). `commonMain/Plugin.kt` rewritten to register both realizations and
  bind `CacheSimpleRolesFeature` as `SimpleRolesFeature`.

### 1.4 Client + server wiring

- `client/build.gradle` — added `api project(":wishlist.features.simpleRoles.client")`.
- `client/src/jsMain/kotlin/Main.kt`, `client/src/jvmMain/kotlin/Main.kt`,
  `client/android/src/main/kotlin/MainActivity.kt` — added
  `dev.inmo.wishlist.features.simpleRoles.client.{JSPlugin,JVMPlugin,AndroidPlugin}` to each platform's
  plugin list (`client/android/build.gradle` needs no change — it only depends on `:wishlist.client` and
  gets `simpleRoles.client` transitively).
- `server/sample.config.json`, `server/dev.config.json` — appended
  `dev.inmo.wishlist.features.roles.server.JVMPlugin` and
  `dev.inmo.wishlist.features.simpleRoles.server.JVMPlugin` to each file's `"plugins"` array (verified
  the FQCN convention against the existing `email.server.JVMPlugin` entry already present in both
  files). `server/local.config.json` intentionally left untouched — it is gitignored
  (`.gitignore:14 local.*`) and not in the spec's file list.

### 1.5 READMEs

- `features/roles/README.md`, `features/simpleRoles/README.md` — created new, `## Operator Notes`
  present and empty (per `agents/ALL.md`'s Feature README Rule — agents must not write operator
  content). Overview/Routes/Models/Architecture Notes bodies are the architecture spec's byte-exact §5
  text (describing the feature's complete, final 4-pass design, per the spec's own reasoning for writing
  READMEs in full during pass 1). Added one clearly-delineated extra blockquote at the end of each
  ("Coding-pass status note") explaining what pass 1 actually landed vs. what is still pending in passes
  2–4, so a reader mid-rollout isn't misled into thinking `FeatureRolesRegistry`/bootstrap/call-site
  replacement already exist — this is the one deliberate deviation from the spec's literal §5 text, and
  it does not alter or remove anything the spec asked for.

## 2. Deviation from the spec: one additional file, and why

**`build.gradle` (root)** was modified — **not** in the architecture spec's Pass 1 file list — to add a
`configurations.all { resolutionStrategy { force ... } }` block inside `allprojects`, forcing
`androidx.core:core` and `androidx.core:core-ktx` to `1.18.0`.

**Why:** the mandated `microutils` `0.29.4` → `0.30.0` bump (required for kroles 0.0.2, not optional)
transitively bumps `dev.inmo:micro_utils.repos.common-android-debug`'s requested
`androidx.core:core(-ktx)` from (transitively) `1.18.0` to `1.19.0`. `androidx.core:core(-ktx):1.19.0`'s
AAR metadata requires `compileSdk 37` **and** AGP `9.1.0+`; this project pins `android-compileSdk = "36"`
and `android-gradle-plugin = "8.12.+"` (`gradle/libs.versions.toml`). `:wishlist.client.android:checkDebugAarMetadata`
failed with 4 AAR-metadata errors as a direct result. Bumping `compileSdk` alone would not have fixed
it (the AGP-9.1.0+ requirement is a hard `minAgpVersion` AAR-metadata constraint, not a soft
recommendation) — a real AGP major-version bump was judged out of scope and too risky for a foundation
pass. Verified via `git stash` that the failure did **not** exist before this pass's `microutils` bump
(baseline `checkDebugAarMetadata` passes cleanly on `0.29.4`, where `androidx.core:core(-ktx)` already
resolves to `1.18.0` across the whole dependency graph — same version now forced). This is a "requires"
(not "strictly") version constraint in microutils' Gradle module metadata, so forcing back to `1.18.0` —
the version every other AndroidX dependency in this project's graph already converges on — is a safe,
minimal override; nothing in this app's own code (old or new) touches a 1.19.0-only API. This was the
one build-fix cycle taken per `agents/CODING.md`'s "do only one cycle of fixing" rule.

## 3. Build result

Ran in the foreground, waited for completion (not backgrounded):

1. `ast-index rebuild` — 719 files, 52 modules indexed, clean.
2. `./gradlew :wishlist.features.roles.common:build :wishlist.features.roles.server:build :wishlist.features.roles.client:build :wishlist.features.simpleRoles.common:build :wishlist.features.simpleRoles.server:build :wishlist.features.simpleRoles.client:build`
   — first attempt failed on `:kotlinStoreYarnLock` (stale `kotlin-js-store/yarn.lock`, gitignored,
   infra-only — unrelated to this pass's code). Fixed with `./gradlew kotlinUpgradeYarnLock` (one-shot,
   no tracked-file change). Re-run: **BUILD SUCCESSFUL**, `simpleRoles.server`'s 3 unit tests passed
   (`isSuperAdminReturnsTrueForSubjectWithSuperAdminRole`, `isSuperAdminReturnsFalseForSubjectWithOnlyUserRole`,
   `isSuperAdminReturnsFalseForUnknownUser` — all green, 0 failures).
3. `./gradlew build` (full project) — first attempt failed on `:wishlist.client.android:checkDebugAarMetadata`
   (see §2). After the `androidx.core` force fix: **BUILD SUCCESSFUL**, 4403 actionable tasks
   (2075 executed, 2328 up-to-date). Confirmed **zero test failures** project-wide: scanned all 29
   `TEST-*.xml` files under every module's `build/test-results/` for `failures="[1-9]` /
   `errors="[1-9]` — no matches.

Manual runtime verification (boot server + curl the new endpoint) was **not** performed this pass —
out of scope for a scaffolding-only pass with no database migration yet, and the spec's own Pass-1
"Verify" section only calls for build+unit-test green at this stage plus a manual check that is
meaningful once Pass 2's bootstrap exists to populate the `roles` table. Deferring that manual
`curl -H "Authorization: Bearer ..." /api/simpleRoles/isSuperAdmin` check to whichever pass runs the app
next (it will return `false` for everyone right now, including `root`, since the `roles` table is empty
until Pass 2 lands — this is expected, not a defect).

## 4. What Pass 2 (bootstrap + migration) needs from this pass

Everything Pass 2 needs already exists and is verified working:

- `RolesRepo` (cache-backed, Exposed-backed) is resolvable from Koin once `roles.server.JVMPlugin` runs
  — already true today, since that plugin is already in both server config files' `"plugins"` array.
- `roles/server/build.gradle` still needs its two extra deps added by Pass 2 itself
  (`users.common` for `UsersRepo`/`RegisteredUser`, `auth.server` for
  `getCallerUserIdOrAnswerUnauthorized`, used later by Pass 3's `RequireRole.kt` — not by Pass 2's own
  code, but the spec bundles adding it in Pass 2 since it's the same file).
- `roles/server/src/jvmMain/kotlin/JVMPlugin.kt` is currently the stock delegating scaffold — Pass 2
  replaces its body with the real subscribe-then-backfill bootstrap (`RolesBootstrap.kt` +
  `VersionsRepo`-gated migration), per architecture spec §3.4/§4 Pass 2.
- `SuperAdminRole`/`UserRole` (in `roles/common/RoleConstants.kt`) are already in place for Pass 2's
  `grantDefaultRoles` to reference directly.
- No schema migration is needed for the `roles` table itself — `ExposedKeyValuesRepo`'s own `init`
  block already calls `initTable()` (confirmed by reading `ExposedReadKeyValuesRepo.kt`), so the table
  exists (empty) as soon as `roles.server.JVMPlugin` first runs `setupDI`, which it already does today.

## 5. Verification against the architecture spec

Every new file's content was cross-checked against the actual library sources this pass, not trusted
from the spec's prose alone (mirroring the spec's own §1 methodology): `kroles.roles`/`kroles.repos`
API (`BaseRole`, `BaseRoleSubject`, `RolesRepo`/`ReadRolesRepo`/`WriteRolesRepo`, `CacheRolesRepo`,
`KeyValueRolesRepo`) read directly from `/home/aleksey/projects/own/kroles`; MicroUtils
`ExposedKeyValuesRepo`/`withMapper`/`singleWithBinds`/`SmartRWLocker` read directly from
`/home/aleksey/projects/own/MicroUtils`; `getCallerUserIdOrAnswerUnauthorized`, `meStateFlow`
(`Koin`/`Scope` accessor pair), and the bare `authenticate { }` route-wrapping convention (no named
provider) cross-checked against this repo's own `features/auth/server`, `features/auth/client`, and
`features/admin/server/.../AdminRoutingsConfigurator.kt`. No discrepancies found between the spec's
byte-exact code and the actual library/repo source.
