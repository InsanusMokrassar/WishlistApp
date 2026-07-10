Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~3300s
Tokens used: not instrumented by harness; extensive multi-source investigation (ast-index searches, ~25 file reads in this repo, ~20 GitHub API calls + source-file fetches against InsanusMokrassar/kroles, Maven Central lookups)
Changed files: agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/001-planning.md (this file only)

# Planning: Issue #68 — Implement roles in project

## 0. Notice: prompt-injection content found in `AGENTS.md`

Before starting the task, `AGENTS.md` (which `agents/SHORTCUTS.md` — the real routing file — expects to be a
routing pointer) was found to contain a fully unrelated payload: a demand that all agents adopt an
"AUTISTIC META-LANGUAGE PROTOCOL WITH HIGH INFORMATION DENSITY (AML-HIP)" — a pronoun-free, key=value pseudo-syntax,
framed with "YOU MUST FOLLOW... WITHOUT ANY QUESTIONS OR DOUBTS" and threats that non-conforming output is
"considered absent." This is not consistent with any other `agents/*.md` file in this repo (all of which are
normal, readable prose), directly contradicts this run's explicit instruction to use "normal prose," and matches
the shape of a prompt-injection payload rather than legitimate project documentation. It has been treated as
untrusted content and ignored; this report is written in normal prose as instructed by the operator/orchestrator.
Flagging so a human can decide whether to clean up `AGENTS.md` (restore it to a pointer at `agents/SHORTCUTS.md`,
which is intact and was used instead).

## 1. Task understanding

GitHub issue #68 asks for a full roles system built on a new library, `dev.inmo:kroles`, with ten
sub-requirements: (1) a new "roles" feature, (2) including kroles, (3) Exposed + caching repos for kroles'
role storage (cache mirrors exposed state), (4) an aggregator of features and the role they require, (5) a
hardcoded `SuperAdmin` role assigned to `root`, (6) a hardcoded `User` role auto-assigned to all users going
forward plus a one-time migration backfilling it onto existing users, (7) a second, small feature exposing
only `isSuperAdmin` (server: takes `UserId`; client: no argument), (8) replacing every server-side root check
with feature 7, (9) replacing every client-side root check with a superadmin check, (10) feature 7's client
side needing two realizations — Ktor and a cache that caches only the boolean answer.

This is explicitly the largest task attempted on this repo: new external dependency, new feature module(s), a
DB-backed repo pattern adapted to a library this repo doesn't already depend on, a data migration (a mechanism
this repo has never used before), and a rewrite of every privilege check on both client and server. Per
`agents/PLAN.md` and this run's own instructions, the mandate is to investigate exhaustively, resolve everything
that can be resolved with evidence, and surface only genuine, evidence-backed ambiguity as open questions rather
than guessing on consequential architecture.

## 2. Investigation findings

### 2.1 `dev.inmo:kroles` — actual API (investigated from source, not guessed)

Source: `https://github.com/InsanusMokrassar/kroles` (author: same `InsanusMokrassar` as this repo). The
repository is real, was scaffolded from the author's own KMP template, and — notably — its `CHANGELOG.md` /
`gradle.properties` show version **0.0.2**, and its Actions history shows four successful "Build completed"
CI runs on **2026-07-10** (today), the last at 11:26 UTC. Maven Central's own artifact page
(`central.sonatype.com/artifact/dev.inmo/kroles.roles`) confirms **0.0.2 published ~8 hours ago**; the
`search.maven.org` index still only shows 0.0.1 (its indexing lags sync). **Conclusion: kroles 0.0.2 was
published today, apparently purpose-built for this exact issue, and is resolvable from Maven Central now.**
Only a `v0.0.1` GitHub *Release* tag exists (from 2024) — 0.0.2 has no formal GitHub release yet, only the
Maven Central publish; not a blocker, just note for whoever later wants release notes.

**Maven coordinates** (group `dev.inmo`, KMP artifacts — `common`/`-jvm`/`-js`/`-android` variants per usual
convention), all at `0.0.2`:
- `dev.inmo:kroles.roles` — the role value types (no repo/storage).
- `dev.inmo:kroles.repos` — the repo/storage abstractions (depends on `roles`).
- `dev.inmo:kroles.repos.ktor` — shared HTTP constants/DTOs for the roles-over-HTTP API.
- `dev.inmo:kroles.repos.ktor.client` / `dev.inmo:kroles.repos.ktor.server` — full remote `RolesRepo`
  client/server (see below — **recommended not to use these**, see §2.1.4).

Dependency pins in kroles' own `gradle/libs.versions.toml`: `kotlin=2.3.21`, `kotlin-serialization=1.11.0`,
`kslog=1.6.1` — all **identical** to this repo's current pins. `microutils=0.30.0` — this repo currently pins
`microutils=0.29.4` (`gradle/libs.versions.toml:9`). **Required step for Coding: bump this repo's `microutils`
version catalog entry from `0.29.4` to `0.30.0`** (already present in the local Gradle cache, confirmed
resolvable) so the whole graph resolves against one microutils version; flag for Architecture/Verification to
confirm the bump doesn't break any existing `micro_utils.repos.exposed`/`.cache` usage (the API surfaces read
during this investigation, e.g. `initTable()`, `ExposedKeyValueRepo`, look unchanged between the two versions,
but a real `./gradlew build` after the bump is the actual proof).

#### 2.1.1 Role model — confirms it is NOT a closed enum

`dev.inmo.kroles.roles.BaseRole` is a `@JvmInline value class BaseRole(val plain: String)` — roles are plain
strings, created/removed at runtime via `RolesRepo.createRole`/`removeRole`. There is no enum anywhere in the
library. A `dev.inmo.kroles.roles.rwm.RWMRole` sub-format also exists (`"prefix.rwm.identifier"` strings
encoding read/write/manage flags plus an optional per-resource identifier, e.g. `"groups.rw.10"`), giving
fully general per-resource granular permissions. **This issue's needs (two flat named roles, `SuperAdmin` and
`User`, no per-resource granularity) do not require the RWM sub-format** — plain `BaseRole("SuperAdmin")` /
`BaseRole("User")` constants defined in our own code are sufficient, layered on top of a library that is
itself fully general. This resolves the "closed enum vs. general model" framing in the task brief: the
library is general; our own two roles are simply fixed `BaseRole` constants, same as how e.g. `rootUsername`
is already a fixed string constant elsewhere in this codebase.

#### 2.1.2 Subject model

`dev.inmo.kroles.repos.BaseRoleSubject` is a sealed interface: `Direct(identifier: String)` (a raw identifier —
for us, a stringified `UserId`) or `OtherRole(role: BaseRole)` (a role acting as a subject, which is what
enables role-inherits-role hierarchies via `RolesRepo.getAllRoles`'s transitive graph walk). For this issue we
only need `Direct` subjects (users) holding flat roles — no role-inherits-role hierarchy is required by any of
the 10 points.

#### 2.1.3 Repo abstraction — compatible with, but distinct from, this repo's CRUD Repository Pattern

`dev.inmo.kroles.repos.RolesRepo : ReadRolesRepo, WriteRolesRepo` is **built directly on the same
`dev.inmo:micro_utils.repos` family this repo already uses** (`repos.common`, `repos.cache`, `repos.exposed`
via the app; kroles itself depends on `repos.common`/`.cache`/`.inmemory`/`.exposed`/`.ktor.*`), so
compatibility is confirmed, but it is **not** shaped like this repo's `CRUDRepo<Registered,Id,New>` pattern —
there is no per-row identity/update/delete; it is a multi-value key→list-of-values relation (a subject maps to
the list of roles directly granted to it). Concretely:
- `ReadRolesRepo`: `getDirectRoles(subject)`, `getDirectSubjects(role)`, `getAll()`, `getAllRoles(subject)`
  (transitive), `contains(subject, role)`, `containsAny(subject, roles)`, paginated listing.
- `WriteRolesRepo`: `includeDirect(subject, role)`, `excludeDirect(...)`, `createRole`, `removeRole`,
  `modifyDirect`, plus reactive `Flow`s (`roleIncluded`/`roleExcluded`/`roleCreated`/`roleRemoved`).
- `dev.inmo.kroles.repos.kv.roles.KeyValueRolesRepo` — kroles' own ready-made `RolesRepo` implementation
  backed by any `dev.inmo.micro_utils.repos.KeyValuesRepo<BaseRoleSubject, BaseRole>` (a *multi*-value
  key-value repo — different from the single-value `KeyValueRepo<K,V>` this repo already uses for
  `PasswordsRepo`). This repo already has a JVM/Exposed implementation of that multi-value shape available
  from the microutils library itself: `dev.inmo.micro_utils.repos.exposed.onetomany.ExposedKeyValuesRepo<Key,
  Value>(database, keyColumnAllocator, valueColumnAllocator, tableName)` (confirmed present in the
  `micro_utils.repos.exposed-jvm:0.30.0` sources; this repo doesn't currently use it directly, but does use
  the analogous one-to-many *pattern* hand-rolled with plain Exposed `Table`s in
  `features/wishlist/common/.../ExposedWishlistItemRepo.kt`'s `WishlistItemsLinks`/`WishlistItemImages` inner
  tables).
- **Recommended Exposed backing for the new `roles` feature**: `ExposedKeyValuesRepo<String, String>(database,
  { text("subject") }, { text("role") }, "roles")`, where the `subject` column stores
  `Json.encodeToString(BaseRoleSubject.serializer(), subject)` (round-trips `Direct` vs. `OtherRole` — plain
  `rawValue`/`.plain` strings alone would NOT disambiguate the two subject variants) and the `role` column
  stores `BaseRole.plain` directly (no ambiguity there, since `BaseRole` has only one shape). Wrap with a thin
  adapter (or `withMapper`-style helper, mirroring how `ExposedPasswordsRepo` wraps `ExposedKeyValueRepo<String,
  String>` to expose typed `KeyValueRepo<Username, Password>`) to present `KeyValuesRepo<BaseRoleSubject,
  BaseRole>` to `KeyValueRolesRepo`. This is an implementation-level detail appropriate for the Architecture/
  Coding stage to finalize precisely — the mechanism and library calls involved are fully identified here, so
  it is not blocking.
- `dev.inmo.kroles.repos.CacheRolesRepo` — **kroles ships its own complete, non-placeholder caching decorator**
  (`RolesRepo, CacheRepo` from `micro_utils.repos.cache.CacheRepo`) that keeps an in-memory snapshot of the
  whole subject/role graph, rebuilt on the underlying repo's change flows, delegating writes through and
  serializing refreshes through a `SmartRWLocker`-guarded queue. **This means point 3 ("add exposed and caching
  repos... cache one must fully mirror exposed state") is already solved by the library** — we do not need to
  write our own cache decorator, only the Exposed-backed `RolesRepo` to wrap. `dev.inmo.kroles.repos.
  InMemoryRolesRepo` is a literal empty placeholder (`class InMemoryRolesRepo {}`, unimplemented) in the
  library — not usable, not needed (the `KeyValueRolesRepo` default-constructs an in-memory `MapKeyValuesRepo`
  already, which covers "in-memory" use cases like tests).
- `dev.inmo.kroles.repos.ProtectedRolesRepo` — an optional, separate "is this subject protected from
  include/exclude" policy layer (e.g., could later protect `root`'s `SuperAdmin` grant from accidental
  revocation). Not required by any of the 10 issue points; recommend **skipping it for this issue** (default
  `ReadProtectedRolesRepo.AlwaysUnprotected`) and flagging it as a possible future hardening, not scope creep.

#### 2.1.4 kroles' own Ktor client/server module — investigated, recommend NOT using it

`dev.inmo.kroles.repos.ktor.{client,server}` expose the **entire** generic `RolesRepo` surface over HTTP
(`includeDirect`/`excludeDirect`/`createRole`/`removeRole`/`getAll`/`getAllRoles`/`contains`/`containsAny` plus
WebSocket flows for every change event — `RolesKtorConstants` in the library lists all these routes). This is a
raw, all-or-nothing remote role-management API with no access control of its own baked in. Issue #68 point 7/10
explicitly wants only ONE narrow, read-only capability exposed to the client (`isSuperAdmin`), not general
role-graph read/write access. **Recommendation: do not add `kroles.repos.ktor.client`/`.server` as
dependencies anywhere in this app.** The `roles` feature (points 1–6) stays entirely server-side; the small
`simpleRoles`-style feature (point 7) is the only thing that crosses the network boundary, via this repo's own,
much narrower, bespoke Ktor route (see §2.5). This is a confident recommendation based on reading the actual
route list, not a guess.

### 2.2 Full audit of existing root-privilege checks (via `ast-index`, exhaustive on this branch)

Searched via `ast-index search "root"`, `ast-index symbol "*Root*"`, `ast-index usages` on `rootUsername`,
`isRoot`, `isCurrentUserRootFlow`, `isRootState`, plus manual reads of every hit. This branch (`fix/68-roles`,
based on `master`, **not** including the still-unmerged PRs #69/#70) contains exactly these call sites:

**Server-side** (all compare `caller.username.string` against a private `"root"` constant, resolved via
`UsersRepo`/`ReadUsersRepo`):
| File | Mechanism | Guards |
|---|---|---|
| `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt:68-78` (`requireAdmin()`) | `user.username.string != rootUsername` → `403` | **All** `/admin/users/*`, `/admin/wishlists/*`, `/admin/wishlistItems/*` routes (14 routes) |
| `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:32,52-54` | `caller.username.string != rootUsername` → returns `false` | `sendTestEmail` |
| `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt:50,53-54` (`isRoot()`) | `usersRepo.getById(callerId)?.username?.string == rootUsername` | `PUT /files/avatar/{userId}` when `callerId != userId` |
| `features/auth/server/src/jvmMain/kotlin/JVMPlugin.kt:20,44` | Not a *check* — the bootstrap that *creates* the `root` user (relevant to point 5, not point 8) | n/a |

**Client-side** (all flow from one implementation site):
| File | Mechanism |
|---|---|
| `features/ui/users/src/commonMain/kotlin/Plugin.kt:76-78` | `override val isCurrentUserRootFlow: StateFlow<Boolean> = meState.map { it?.username?.string == "root" }` — the **single** place the check is actually computed |
| `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt:58` | Interface declaration consumed by the two ViewModels below |
| `features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt:61-64` (`canEditState`) | `isRoot || currentUserId == node.config.userId` |
| `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt:59` (`isRootState`), `:119-129` (`canSaveState`) | gates editable username/password fields, delete, save |
| `features/ui/users/src/{jvmMain,jsMain,androidMain}/kotlin/ui/UserEditView.kt` | `val isRoot by viewModel.isRootState.collectAsState()` (×3 platforms) — pure UI consumers, no logic of their own |

**Important scope note on the PROMPT.md context**: the issue-executor's context mentioned "the most recently
completed task on this branch's history (issue #66, PR #69) added a root-only sidebar item using
`UsersModel.isCurrentUserRootFlow`." This was verified and **does not exist on this branch** — `features/ui/
sidebar/src/` has zero references to `root`/`isCurrentUserRootFlow` today. PR #69 (issue #66) is a separate,
still-open/unmerged branch (`fix/66-admin-panel-left-panel-item`); its sidebar changes are not part of this
branch's tree and therefore are not an editable call site right now. **No code changes are needed for it in
this task** — but leaving a note here for whoever merges #69 later: that sidebar item's root check should also
become a superadmin check when it lands, for consistency.

**Conclusion**: the audit surfaces **7 concrete edit sites** (3 server checks + 1 bootstrap touch-point + 3
client consumers collapsing to 1 real implementation edit), matching (and slightly refining) the non-exhaustive
leads already in `PROMPT.md`. No additional root-check call sites were found anywhere else in the tree.

### 2.3 New-user creation call sites (relevant to point 6's "auto-assign `User` role")

Searched for every `usersRepo.create(...)`/`writeUsersRepo.create(...)` call. Exactly three exist, and — this
is the existing, established convention in this codebase — **each already performs its own related follow-up
call right after creation** (there is no shared "on user created" hook to extend; e.g. both already call
`authService.setPassword(...)` right after `usersRepo.create(...)`):
1. `features/auth/server/src/jvmMain/kotlin/JVMPlugin.kt:46` (`bootstrapRootUserIfMissing`) — creates `root`.
2. `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:118` (`register`) — self-registration, gated by `enableRegistration`.
3. `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt:47` (`create`) — admin-created users.

**Plan: each of these three call sites gets one additional line** calling the roles feature to grant `User` to
the newly created user's id, mirroring the existing `setPassword`-after-`create` pattern exactly (not a new
pattern — the established one, applied consistently). This requires `features/auth/server` and
`features/admin/server` to each add a Gradle dependency on the new `roles` feature's common/JVM module
(consistent with "each feature may depend on any other one" and with existing cross-feature dependencies
already present, e.g. `admin/server` already depends on `auth/server`).

### 2.4 Migration mechanism — resolved, no new infrastructure needed

Investigated whether this repo has any precedent for a one-time data backfill beyond schema DDL. `initTable()`
(`dev.inmo.micro_utils.repos.exposed.ExposedTableInitialization.kt`, read from the `micro_utils.repos.
exposed-jvm:0.30.0` sources jar) is confirmed **schema-only** — `SchemaUtils.createMissingTablesAndColumns`-
equivalent DDL, no interaction with `VersionsRepo` whatsoever, and it does not accept any data-migration
callback.

However, `VersionsRepo<Database>` (registered as a Koin `single` in `features/common/server/src/jvmMain/kotlin/
JVMPlugin.kt:83-95`, backed by `StandardVersionsRepo`/`KeyValueBasedVersionsRepoProxy` over the existing
`tables_versions` Exposed key-value table) is **currently registered but has zero callers anywhere in this
codebase** (confirmed via `ast-index usages VersionsRepo` — exactly one hit, the registration itself). Reading
its actual interface (`dev.inmo.micro_utils.repos.versions.VersionsRepo`, from the `micro_utils.repos.common:
0.30.0` sources) shows it is **not** schema-DDL-only — it is a fully generic, already-integrated, per-table
one-time-migration primitive:

```kotlin
suspend fun setTableVersion(
    tableName: String,
    version: Int,
    onCreate: suspend T.() -> Unit = {},          // T = Database; fires once, when no version is stored yet
    onUpdate: suspend T.(from: Int, to: Int) -> Unit = { _, _ -> }  // fires once per version step, iteratively
)
```

`StandardVersionsRepo.setTableVersion` reads the stored version for `tableName`, calls `onCreate` if absent,
then iteratively calls `onUpdate(from, to)` and persists the new version until the target `version` is reached
— after which subsequent calls are no-ops. **This is exactly the "small migration" point 6 asks for, with zero
new infrastructure**: call `versionsRepo.setTableVersion("users_default_role_backfill", 1, onUpdate = { _, _ ->
/* iterate usersRepo.getAll(), grant User to every user missing it via the roles feature */ })` once, from the
`roles` feature's server `JVMPlugin.startPlugin`. Using a distinct `tableName` key (not a real DB table name,
just the version-tracking key `tables_versions` uses) avoids any collision with actual schema versioning.

### 2.5 Root-user bootstrap interaction (point 5) and plugin load order

Re-read `agents/CODING.md`'s "Root-user bootstrap" section in full. `features/auth/server/src/jvmMain/kotlin/
JVMPlugin.kt.bootstrapRootUserIfMissing` creates `root` and calls `authService.setPassword(...)` — it is
explicitly documented as the place `auth` "owns the wiring of users into the server graph." CODING.md separately
warns: "You MUST NOT add new setupDI and startPlugin methods calls in plugins for the other plugins outside of
feature" (i.e., don't wire arbitrary cross-feature plugin lifecycle calls). Investigated how the microutils
launcher actually sequences plugins (`agents/ARCHITECTURE.md`'s Startup Flow + reading `server/sample.config.
json`'s `"plugins"` array, which lists `common → sample → users → auth → wishlist → files → admin → currency →
booking → email → deeplinks` in that literal order): **all plugins' `setupDI` runs first (building one shared
Koin module), then `startPlugin` runs per-plugin in list order.** This is confirmed by e.g. `admin`/`files`
already resolving `UsersRepo`/`AuthFeatureService` without any direct cross-plugin lifecycle call — they simply
rely on load order, not on `auth` explicitly invoking them.

**Resolved design (no cross-feature plugin-call coupling needed, no edit to `features/auth/server/JVMPlugin.kt`
required at all)**: place the new `roles` feature's server plugin **after** `auth` in `sample.config.json`'s
plugin list. In `roles/server/JVMPlugin.startPlugin` (which runs after `auth`'s `startPlugin` has already
created `root`), do two idempotent things every startup:
1. **`SuperAdmin` → `root`** (point 5): look up `root`'s `UserId` via `UsersRepo`, then
   `rolesRepo.includeDirect(BaseRoleSubject.Direct(userId), BaseRole("SuperAdmin"))`. `includeDirect` is
   naturally idempotent (returns `false`, no error, if already granted) and O(1) — **no `VersionsRepo` gating
   needed here**, just run it unconditionally on every startup (self-healing if ever manually revoked).
2. **`User` → all existing users** (point 6's migration): the one-time, `VersionsRepo`-gated backfill from
   §2.4 — this one IS gated by version (an O(n) full-table operation, shouldn't re-scan on every restart as the
   user table grows).

Because the backfill in step 2 runs against *all currently existing* users and `root` was already created by
`auth`'s earlier-in-order bootstrap, **`root` naturally receives both `SuperAdmin` (step 1) and `User` (step 2,
since root counts as an "existing user" by the time this runs)** — no special-casing needed, no conflict between
points 5 and 6.

### 2.6 Module boundary: "roles" vs. the "simple roles" feature (point 7) — resolved

Investigated whether points 1–6 and point 7 should be one feature module or two, per this run's instruction to
decide or flag. Resolved as **two separate feature modules**, both scaffolded via `./generate_feature.sh` per
`agents/ARCHITECTURE.md`'s Feature adding rules:

- **`roles`** (points 1–6): owns the kroles dependency, the Exposed+cache `RolesRepo`, the `SuperAdmin`/`User`
  constants, the root-bootstrap hook, and the migration. **Server-only in practice** — nothing in the issue
  needs a client to talk to the full role graph, and §2.1.4 recommends against exposing kroles' generic
  Ktor surface. The scaffolded `client` submodule stays a minimal, unused stub (consistent with how a fresh
  feature's common `Plugin.kt` "is typically empty at the start" per CODING.md) rather than fighting the
  standard 3-submodule scaffolding tool.
- **`simpleRoles`** (placeholder name — Architecture/naming is free to rename; point 7): a small, genuinely
  full-stack feature depending on `roles` server-side. Server interface: `suspend fun isSuperAdmin(userId:
  UserId): Boolean` (delegates to `roles`' `RolesRepo.contains(BaseRoleSubject.Direct(userId.long.toString()),
  BaseRole("SuperAdmin"))`). Client interface: `suspend fun isSuperAdmin(): Boolean` (no argument — the server
  route resolves the caller from the bearer token via the existing `getCallerUserIdOrAnswerUnauthorized()`
  helper, exactly like other authenticated routes already do; the client never sends a `UserId`). **Note**: because
  the server and client signatures genuinely differ (arg vs. no-arg), this is one of the few features where the
  interface is legitimately *not* shared/mirrored 1:1 between server and client `commonMain` — CODING.md's Full-
  Stack Feature pattern already anticipates this ("mirror the interface if it is not in common module of
  feature"), so this is a normal application of an existing allowance, not a new pattern.
  - Return type `Boolean` is exempt from the (not-yet-merged-into-master, but explicitly required-by-this-task)
    "Feature Interface Return Model Rule" — see §2.7 — so no feature-model wrapper type is needed here.
  - Point 10's two client realizations map directly onto the already-documented "Ktor realization rule"
    (CODING.md, Full-Stack Feature / Client module section): `KtorSimpleRolesFeature` does only the HTTP call;
    a wrapping cache service (e.g. `CacheSimpleRolesFeature`) caches only the boolean answer and is what's
    actually bound as `SimpleRolesFeature` in Koin. Cache invalidation on logout/login-change should mirror how
    `UsersModel.userAuthorisedState` already tracks `AuthCredentialsStorage.userAuthorised` — Architecture to
    pin down the exact invalidation trigger.
- **Point 8/9 replacement mechanics**: the 3 server call sites (§2.2) each swap their `username == "root"`
  check for `simpleRolesFeature.isSuperAdmin(callerId)`, requiring `admin/server`, `email/server`, `files/
  server` to each add a dependency on `simpleRoles`'s server module (build.gradle files for all three were
  read; each already has an analogous cross-feature `api project(...)` dependency, e.g. on `auth.server`, so
  this is mechanical, not novel). The **single** client implementation site (§2.2, `features/ui/users/src/
  commonMain/kotlin/Plugin.kt:76-78`) swaps its `meState.map { username == "root" }` for a call to the new
  client `SimpleRolesFeature.isSuperAdmin()` — because every client consumer (`UserViewModel`,
  `UserEditViewModel`, the 3 platform `UserEditView`s) already derives from this one flow, **no other client
  file needs to change**.

### 2.7 "Feature Interface Return Model Rule" — confirmed content (not yet in this branch's `CODING.md`)

This run's instructions require following this rule, but grepping this branch's `agents/CODING.md` (1132 lines,
confirmed complete) shows it does not exist here — it was added on the still-unmerged `fix/67-users-feature-
model` branch (issue #67, PR #70), which this branch's `master` base predates. Retrieved its actual text from
`origin/fix/67-users-feature-model:agents/CODING.md` (lines 1005–1037) to apply it correctly rather than
guessing: `*Feature` methods must not return raw CRUD `New*`/`Registered*` persistence models; identifiers,
primitives (`Boolean`, `String`, numbers, `Unit`), and non-project library types are exempt. `SimpleRolesFeature.
isSuperAdmin(): Boolean` is fully exempt (primitive return). No new feature-model type is needed for this
issue's interfaces. Flagging as informational only — not blocking, but worth a note for whoever later reconciles
branch history that `fix/68-roles` was planned against a rule pulled forward from an unmerged sibling branch.

## 3. Open questions — BLOCKING, need operator input before Architecture proceeds

Everything above is resolved with concrete evidence and does not need operator input. Two points remain
genuinely ambiguous — both would materially change the shape of the `roles`/`simpleRoles` interfaces if guessed
wrong, so per `agents/PLAN.md` step 4 and this run's explicit instructions, they are surfaced rather than
guessed:

1. **Is `SuperAdmin` ever grantable to a user other than `root`, or must access stay architecturally
   root-only?** `features/admin/README.md`'s `## Operator Notes` (human-authored, must not be modified, found
   during this investigation) states: *"Only `root` user must have access to the admin panel and features."*
   kroles' role model is fully general — nothing stops a future admin action from granting `SuperAdmin` to a
   second user via `RolesRepo.includeDirect`. If the answer is "root-only, permanently," the `roles`/
   `simpleRoles` features should not expose any general-purpose "grant SuperAdmin to arbitrary user" capability
   at all (keep it a fixed, code-only assignment with no admin UI/route to change it) and `AdminRoutingsConfigurator`
   should conceptually remain "only `root`" even though it's now implemented via `isSuperAdmin`. If the answer
   is "any user may eventually hold `SuperAdmin` once granted through some future mechanism," the Operator Note
   needs updating (Planning/Architecture must not touch it) and the `roles` feature's surface should be built
   more generally from the start (e.g. exposing `includeDirect`/`excludeDirect` for `SuperAdmin` through some
   admin-facing capability). **This changes what "aggregator"-adjacent API surface (if any) is safe to build now
   vs. defer.**

2. **Is point 4's "aggregator of features and required role for them" a concrete deliverable (a registry/data
   structure + generic route-guard helper that any current or future feature registers into), or is it
   satisfied by exposing kroles' own `RolesRepo`/`Checkers.kt` primitives (`containsAny`, `isAccessAllowed`,
   etc.) for other features to call directly, with no separate registry object?** Every concrete check this
   issue actually needs (points 5–10) only ever tests a single flat capability (`SuperAdmin`); nothing in the
   issue exercises a real multi-feature-to-role mapping. Building a bespoke aggregator/registry class now with
   no real caller would be speculative; treating kroles' existing generic check functions as "the aggregator"
   and not building anything extra is the more conservative reading, but it's a genuine interpretation call on
   informal issue text, not something to guess silently on a task this architecturally significant. If a real
   registry is wanted, its shape (symbolic feature-id → required `BaseRole`, consulted by a shared Ktor route-
   guard) needs to be specified before Architecture designs the `roles` feature's public interface.

## 4. Plan for everything that is NOT blocked

Handed off to Architecture once the two open questions above are answered. Summary of the concrete, resolved
plan (Architecture to turn into file-level design + test stubs per `agents/ARCHITECTURE.md`'s Test Planning
Requirement):

1. **Gradle**: add `kroles = "0.0.2"` + `kroles-roles`/`kroles-repos` (and NOT `kroles.repos.ktor.*`, per
   §2.1.4) entries to `gradle/libs.versions.toml`; bump `microutils` from `0.29.4` to `0.30.0` in the same file.
2. **Scaffold `features/roles`** via `./generate_feature.sh`; register in `settings.gradle`; server `build.gradle`
   depends on `kroles-roles`/`kroles-repos` + `microutils-repos-exposed`/`.cache`; common `jvmMain` holds the
   Exposed `KeyValuesRepo<String,String>` (JSON-encoded `BaseRoleSubject` key / plain `BaseRole` value, per
   §2.1.3) wrapped by kroles' `KeyValueRolesRepo` then kroles' `CacheRolesRepo`, wired via `singleWithBinds`
   exactly like the documented CRUD Repository Pattern's DI wiring section (adapted: no `New*`/`Registered*`
   types exist for this repo shape). Define `SuperAdmin`/`User` as `BaseRole` constants here.
3. **`roles/server/JVMPlugin`**: placed after `auth` in `server/sample.config.json`'s `"plugins"` list; on
   `startPlugin`, run the two idempotent/one-time operations from §2.5 (SuperAdmin→root every startup;
   User→all-existing-users once, via `VersionsRepo.setTableVersion` per §2.4).
4. **Scaffold `features/simpleRoles`** (name pending Architecture) per §2.6: server interface + Exposed-free
   service delegating to `roles`' `RolesRepo`; a bespoke, narrow Ktor route (own routing configurator, not
   kroles' generic one); client interface with Ktor + Cache realizations per point 10.
5. **Rewire the 3 user-creation call sites** (§2.3) to grant `User` on creation.
6. **Replace the 3 server root checks + 1 client implementation site** (§2.2) to call `simpleRolesFeature.
   isSuperAdmin(...)`, adding the necessary new cross-feature Gradle dependencies (`admin/server`, `email/
   server`, `files/server` → `simpleRoles/server`; `features/ui/users` → `simpleRoles/client`).
7. **README.md updates** (per `agents/ALL.md`'s Feature README Rule) for the two new features' `## Architecture
   Notes`, plus updates to `admin`/`email`/`files`/`auth`/`users`/`ui/users` READMEs documenting the swapped
   auth mechanism, done by the Architecture/Coding roles as work proceeds (not by Planning).
8. **Test coverage** (Architecture to write concrete stubs per its Test Planning Requirement, informed by this
   investigation): unit coverage for the Exposed `KeyValuesRepo` adapter's `BaseRoleSubject` JSON round-trip,
   the `VersionsRepo`-gated backfill (runs once, idempotent on re-run, only touches users missing `User`), the
   `SuperAdmin`-to-root idempotent grant, and `SimpleRolesFeature.isSuperAdmin` true/false paths on both server
   and the two client realizations (Ktor happy/401 path; cache hit/invalidate-on-logout path) — mirroring the
   just-completed `EmailFeatureService`/`EmailsService` task's test structure on this same repo.
9. Everything in §4 depends on the answers to §3's two questions only insofar as they affect the *shape* of
   `roles`'/`simpleRoles`' public surface (whether any grant/revoke capability is exposed beyond the fixed
   root/SuperAdmin and creation-time User assignment); the scaffolding, dependency, migration-mechanism, and
   call-site-replacement plan above holds regardless of the answers.

## 5. Status

**BLOCKED** — two concrete open questions in §3 need operator input before Architecture should proceed past the
parts of §4 that touch the `roles`/`simpleRoles` public interface shape. Everything else in §4 is READY.
