# Feature: Roles

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Server-only role storage feature (issue #68, points 1–6) wrapping the external `dev.inmo:kroles`
library. Owns the Exposed-backed, cache-mirrored `RolesRepo`, the two hardcoded roles this app uses
(`SuperAdmin`, `User`), the feature/role aggregator (`FeatureRolesRegistry`) and its route-guard
helper (`requireRole`), and the bootstrap/migration that assigns `SuperAdmin` to `root` and `User` to
every user. Nothing in this feature is exposed to any client — see Architecture Notes.

## Routes

None. This feature has no HTTP surface; the only client-facing capability derived from it
(`isSuperAdmin`) is exposed by the separate `features/simpleRoles` feature.

## Models

| Type | Module | Description |
|------|--------|-------------|
| `SuperAdminRole` | `roles/common` | `BaseRole("SuperAdmin")` constant — the single, hardcoded, root-only administrative role. |
| `UserRole` | `roles/common` | `BaseRole("User")` constant — granted to every registered user. |
| `FunctionalityId` | `roles/common` | `@JvmInline value class FunctionalityId(val string: String)` — strongly-typed id for a capability, replacing raw `String` feature ids. |
| `FeatureRolesRegistry` | `roles/common` | Interface — aggregator of `FunctionalityId -> BaseRole` mappings; `requiredRole(functionalityId): BaseRole?`; realized by `MapFeatureRolesRegistry(getAllDistinct())`. |
| `FeatureRolesRegistry.Requirement` | `roles/common` | `data class Requirement(val functionalityId: FunctionalityId, val role: BaseRole)` — one functionality→role pair contributed via `singleRequirement` into Koin. |
| `MapFeatureRolesRegistry` | `roles/common` | In-memory realization of `FeatureRolesRegistry` built from DI-collected requirements; folds them into a `FunctionalityId -> BaseRole` map at construction; throws `IllegalStateException` if two requirements assign different roles to the same `FunctionalityId`. |
| `singleRequirement` | `roles/common` | Koin `Module` extension — `singleRequirement(createdAtStart: Boolean = false, block: Definition<FeatureRolesRegistry.Requirement>)` — registers one `Requirement` via `singleWithRandomQualifier` so any number can be contributed without qualifier collisions. |
| `RoleGatedFeatureIds` | `roles/common` | `FunctionalityId` vals — symbolic ids for role-gated capabilities (`adminPanel = FunctionalityId("admin.panel")`, `filesAvatarChangeForOthers = FunctionalityId("files.avatarChangeForOthers")`, `emailSendTest = FunctionalityId("email.sendTest")`). |
| `requireRole` / `isRoleRequirementSatisfied` | `roles/server/utils` | Route-guard helper (`RoutingContext.requireRole(functionalityId, registry, rolesRepo)`) and its pure allow/deny decision function (`isRoleRequirementSatisfied(registry, functionalityId, callerId, rolesRepo)`); now take a `FeatureRolesRegistry` instance + `FunctionalityId` (was: static object + `String featureId`). |
| `RolesRepo` (kroles) | `roles/common` (JVM) | kroles' own `RolesRepo` (`dev.inmo.kroles.repos`), bound in Koin as the Exposed+cache-backed implementation — see Architecture Notes. |
| `roles` table | Postgres | Two text columns, `subject` (JSON-encoded `BaseRoleSubject`) and `role` (`BaseRole.plain`); one-to-many, via `ExposedKeyValuesRepo`. |

## Architecture Notes

- **kroles wrapping, not reinvention (issue point 3):** kroles ships its own `CacheRolesRepo` (a
  complete, in-memory-snapshot cache decorator over any `RolesRepo`, rebuilt on the underlying repo's
  change flows). This app only supplies the Exposed-backed `RolesRepo` to wrap
  (`exposedRoleSubjectToRoleRepo` + kroles' `KeyValueRolesRepo`, in
  `roles/common/jvmMain/repo/RolesRepoFactory.kt`) — no bespoke cache decorator was written. The
  Exposed table (`roles`) stores a JSON-encoded `BaseRoleSubject` as the key (`Direct`/`OtherRole`
  need round-tripping — the plain `rawValue` string alone would not disambiguate the two) and the
  plain `BaseRole.plain` string as the value, via the MicroUtils `withMapper` adapter (mirrors how
  `ExposedPasswordsRepo` wraps `ExposedKeyValueRepo<String, String>`).
- **`roles/client` is a deliberate, permanent stub.** The module is scaffolded (this repo's tooling
  always creates all three submodules) but is not added as a dependency of `client/build.gradle` and
  its platform plugins are not registered in any `Main.kt`/`MainActivity.kt`. Nothing in issue #68
  needs a client to talk to the general role graph, and kroles' own `kroles.repos.ktor.*` module
  (which would expose the *entire* role graph read/write over HTTP with no access control) is
  deliberately never added as a dependency anywhere in this app. The only client-facing surface
  derived from roles data is the separate, narrow `features/simpleRoles` feature.
- **Subscribe-then-backfill bootstrap (issue points 5 & 6), and why:** `roles/server/JVMPlugin.startPlugin`
  subscribes to `UsersRepo.newObjectsFlow` *before* reading any snapshot, then runs a
  `VersionsRepo`-gated one-time backfill over `UsersRepo.getAll()`. This ordering is required because
  the microutils launcher runs every top-level plugin's `startPlugin` **concurrently** (one
  `scope.launch` per plugin, joined at the end) — plugin position in `sample.config.json`'s `"plugins"`
  list has no bearing on execution order between different plugins (only `setupDI`, which builds the
  Koin module, is fully sequential/synchronous across all plugins before any `startPlugin` runs). If
  the migration's `getAll()` snapshot happened to run before `features/auth/server`'s root-bootstrap
  finished creating the `root` user, the migration would see zero users, mark itself permanently done,
  and never see `root` again. Subscribing first closes that race: any user created concurrently by
  another plugin (including `root`) is caught by the live subscription even if it beats the backfill's
  snapshot read. The actual per-user grant rule (`grantDefaultRoles` — grant `User` always, plus
  `SuperAdmin` when `username == "root"`) is shared by both paths and is idempotent (kroles'
  `RolesRepo.includeDirect` is a no-op when already granted), so double-granting in the overlap window
  between the two paths is harmless.
- **`FeatureRolesRegistry` has real data but `requireRole` has no production caller yet.** The
  registry is populated with today's one real mapping (all three role-gated capabilities require
  `SuperAdmin`), and `requireRole`/`isRoleRequirementSatisfied` are fully implemented and unit-tested.
  However, issue #68 point 8's three concrete replacements (`admin`, `email`, `files`) call
  `SimpleRolesFeature.isSuperAdmin` directly instead of `requireRole`, per the issue's own literal
  text and to keep those three modules scoped to `simpleRoles`'s narrow surface rather than the
  general `roles` `RolesRepo`. Requirements are contributed via `singleRequirement { ... }` and
  collected by `MapFeatureRolesRegistry(getAllDistinct())`; `requireRole` is a real, tested,
  ready-to-use guard for the next role-gated route — this gap between "registry has data" and
  "guard has a caller" is intentional, not an oversight.
- **Registration is via DI aggregation, not mutable `register()`.** Requirements are contributed with
  `singleRequirement { FeatureRolesRegistry.Requirement(...) }` and collected by
  `MapFeatureRolesRegistry(getAllDistinct())`, mirroring the precedent of `ApplicationRoutingConfigurator.Element`s
  aggregated by `InternalApplicationRoutingConfigurator` (see `agents/CODING.md` "DI Aggregation"). Today's three
  role-gated capabilities (`admin`, `email`, `files`) declare their requirements centrally in `roles/common/Plugin.setupDI`,
  avoiding a new Gradle dependency on `roles/common` in those three modules purely for a registration side-effect — keeping
  the security-sensitive call-site-replacement change minimal. However, any feature MAY now self-contribute its own
  `Requirement` from its own `setupDI` (via `singleRequirement`), and the registry aggregates all contributors regardless of
  declaring module. A future feature that actually calls `requireRole` for its own gating should self-register at that point.
- **Why `Database`/`CoroutineScope` are resolved by plain `get()`, not a `with(...) { setupDI }` call:**
  `roles/common/JVMPlugin` does not call `features.common.server.JVMPlugin.setupDI` — per
  `agents/CODING.md`'s Plugin rule ("a plugin can't call `setupDI`/`startPlugin` of another plugin
  outside of its own feature"), only the *types* `features/common/server` has already registered
  (`Database`, `CoroutineScope`) are consumed, exactly like every other feature's Exposed repo in this
  app. This is why `roles/server` must load after (or alongside) `features/common/server` in the
  plugin list — the same constraint every other Exposed-backed feature already has.
- **Dependencies:** `roles/common` depends on `kroles.roles`/`kroles.repos` and (transitively, via
  `features/common/common`) `microutils.repos.exposed`/`microutils.coroutines`/kotlinx-serialization.
  `roles/server` depends on `roles/common`, `features/common/server`, `features/users/common` (for
  `UsersRepo`/`RegisteredUser`), and `features/auth/server` (for `getCallerUserIdOrAnswerUnauthorized`,
  used by `requireRole`).
