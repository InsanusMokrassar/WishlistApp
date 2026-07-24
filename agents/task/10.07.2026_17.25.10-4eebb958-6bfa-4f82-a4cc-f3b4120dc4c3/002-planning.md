Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~5400s
Tokens used: not instrumented by harness; full re-verification pass (ast-index queries, ~35 file reads in this repo, direct reads of local `dev.inmo` sources at `/home/aleksey/projects/own/MicroUtils` and `/home/aleksey/projects/own/kroles` per `agents/local.ALL.md`)
Changed files: agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/002-planning.md (this file only)

# Planning (final): Issue #68 — Implement roles in project

This round finalizes the plan opened in `001-planning.md`, using the operator's answers to the two
blocking questions raised there. All of round 1's investigation (kroles' real API, the root-check
audit, the migration mechanism, the module-boundary decision) still holds and is not repeated in
full here except where this round found a factual correction to it (§0 below — found while turning
round 1's plan into concrete, buildable code, not a re-litigation of round 1's design calls).

## 0. Correction to round 1: plugin `startPlugin` order is NOT sequential — reflected below

Round 1 (`001-planning.md` §2.5) asserted "all plugins' `setupDI` runs first ... then `startPlugin`
runs per-plugin in list order" and built the SuperAdmin-to-root bootstrap and the `User`-role
migration on that assumption (placing `roles` after `auth` in `sample.config.json`'s plugin list).
Verifying this claim directly against the launcher's actual source
(`/home/aleksey/projects/own/MicroUtils/startup/launcher/src/commonMain/kotlin/StartLauncherPlugin.kt:92-111`)
shows this is only half true:

- `setupDI` **is** fully sequential/synchronous across all plugins, and **all** of it completes before
  **any** `startPlugin` runs (confirmed: `koinApp.modules(module { setupDI(...) })` completes, Koin
  starts, and only then is `startPlugin` invoked) — round 1's claim holds for `setupDI`, so nothing
  about type/dependency *availability* in Koin was ever actually at risk.
- `startPlugin`, however, is **not** sequential across top-level plugins:
  ```kotlin
  override suspend fun startPlugin(koin: Koin) {
      koin.get<Config>().plugins.map { plugin ->
          scope.launch { with(plugin) { startPlugin(koin) } }
      }.joinAll()
  }
  ```
  Every plugin listed in `sample.config.json`'s `"plugins"` array gets its own concurrent
  `scope.launch { }` coroutine; the launcher only waits for all of them via `joinAll()`. Position in
  the list has **no bearing** on execution order between *different* top-level plugins. (Sequential
  ordering only exists *within* one plugin's own body, e.g. `auth/server/JVMPlugin.startPlugin`
  calling `users.common.JVMPlugin.startPlugin(koin)` directly before running its own bootstrap — that
  is the sanctioned "Plugin Composition Pattern" for *same-feature* delegation, not applicable across
  features per the "you MUST NOT call setupDI/startPlugin of other plugins outside of feature" rule.)

**Consequence:** round 1's design — `roles/server/JVMPlugin.startPlugin` doing a one-shot "look up
`root`, `includeDirect(SuperAdmin)`" plus a `VersionsRepo`-gated "iterate all users, `includeDirect(User)`"
— is **not guaranteed to run after** `auth/server/JVMPlugin.startPlugin`'s `bootstrapRootUserIfMissing`.
On a fresh database, both plugins' `startPlugin` coroutines start at roughly the same moment; `roles`'s
work is a handful of already-cheap Koin lookups plus (for the migration) one `getAll()` query, while
`auth`'s bootstrap does a `usersRepo.count()` check, `SecureRandom` generation, a `usersRepo.create(...)`
insert, and a BCrypt hash+insert for the password — realistically slower. If `roles`'s one-time,
`VersionsRepo`-gated migration's `getAll()` snapshot happens to run **before** `auth` finishes creating
`root`, the migration would see zero users, mark itself done, and **never run again** — permanently
missing `root` for the `User` backfill (the every-boot, unconditional SuperAdmin grant in round 1's
design is self-healing across restarts and would eventually catch up, but the one-time migration is
not). This is a genuine correctness gap in round 1's mechanics, not a stylistic nitpick, and it is
fixed below without needing operator input (it is a factual/technical correction, not a design-taste
question) — full design in §4.

## 1. Recap: operator's answers and how they resolve round 1's two blocking questions

- **Q1 (SuperAdmin scope) → root-only, architecturally.** No admin UI/route will ever grant `SuperAdmin`
  to a second user; `roles`/`simpleRoles` expose **no** grant/revoke capability beyond the fixed,
  code-only root assignment (§3) and the fixed, code-only `User`-on-creation assignment (§4). This
  matches `features/admin/README.md`'s Operator Notes ("Only `root` user must have access") exactly —
  no README Operator Notes edit is needed; if anything, `admin`'s Architecture Notes should later
  document that this is now *enforced via a role check* rather than *implemented as* a username
  comparison, while the operator's constraint itself is unchanged and still holds.
- **Q2 (aggregator scope) → build a real registry + guard helper.** Full design in §2.3. Not wired
  into the 3 concrete point-8 replacements (those use `simpleRoles.isSuperAdmin` directly, per point
  8's literal text and to keep `admin/server`/`files/server`/`email/server` scoped to the narrow
  `simpleRoles` surface rather than the general `roles` `RolesRepo`) — the registry is populated with
  today's one real mapping as data, and the guard helper is proven via direct unit tests. This
  divergence between "the registry has real data" and "the guard helper has no production caller yet"
  is intentional and flagged explicitly in §2.3, not smoothed over.

## 2. `roles` feature (server-only in practice)

Final name: `roles`. Scaffold via `./generate_feature.sh` (enter `roles`) exactly like every other
feature — this always creates all three submodules (`common`, `server`, `client`); there is no
partial-scaffold option in this repo's tooling.

### 2.1 Gradle module registration

`settings.gradle` — add, following the existing block style/blank-line separation:
```groovy
":features:roles:common",
":features:roles:server",
":features:roles:client",
```
Place after the `":features:deeplinks:*"` block and before the `":features:ui:*"` block (end of the
full-stack feature list), matching the file's existing grouping.

**Deliberate deviation from the generic "Feature adding rules" steps 3/5:** `roles/client` is scaffolded
(mandatory, per the tooling) but stays the empty stub the template produces — it is **not** added as a
dependency of `client/build.gradle`, and its platform plugins are **not** added to
`client/src/{jsMain,jvmMain}/kotlin/Main.kt` / `client/android/.../MainActivity.kt`. Nothing in this
issue needs the client to talk to `roles`' general role graph (round 1 §2.1.4: kroles' own Ktor
client/server exposes the *entire* role graph with no access control and is deliberately not used
anywhere in this app); the only client-facing surface is `simpleRoles` (§3), which is fully wired
normally. This mirrors CODING.md's own observation that "the common `Plugin.kt` is typically empty at
the start" — here the *whole client submodule* stays at that starting point, permanently, by design.
Flag this explicitly in `roles/README.md`'s Architecture Notes (§8) so a future reader doesn't assume
it's an oversight.

### 2.2 `roles/common` — model, repo wiring, defaults

**`commonMain/kotlin/RoleConstants.kt`** (KMP-safe — `BaseRole` is a plain `@JvmInline value class`
shipped by `kroles.roles`, itself multiplatform):
```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/** The single, hardcoded, root-only administrative role (issue #68 point 5). Never grantable through
 *  any UI/route in this app — see `roles/README.md` Architecture Notes and `features/admin/README.md`
 *  Operator Notes. */
val SuperAdminRole = BaseRole("SuperAdmin")

/** The single, hardcoded role every registered user holds (issue #68 point 6). Granted automatically
 *  on user creation and backfilled once for pre-existing users — see `roles/server/JVMPlugin`. */
val UserRole = BaseRole("User")
```

`roles`' own `commonMain/kotlin/Plugin.kt` (the scaffold default, generally left near-empty per
CODING.md) additionally registers the feature-role registry entries this issue actually has (§2.3) —
this is the only content added to the otherwise-stock scaffolded `Plugin.kt`.

**`jvmMain/kotlin/repo/RolesRepoFactory.kt`** (or inline in `JVMPlugin.kt` — Architecture's call; shown
separately here for clarity) builds the concrete `dev.inmo.kroles.repos.RolesRepo` used everywhere else
in this app. No app-specific `RolesRepo`/`ReadRolesRepo`/`WriteRolesRepo` reinterpretation is created —
kroles' own `RolesRepo` interface (`BaseRoleSubject`/`BaseRole`, already fully typed, no
`ItemId`/`NewItem`-shaped domain modeling needed) is bound directly, which also means the "Feature
Interface Return Model Rule" doesn't apply here (that rule targets *this app's own* `*Feature`
interfaces returning raw CRUD `New*`/`Registered*` types; kroles' library types are exempt as
"non-project library types").

Confirmed (read directly from `/home/aleksey/projects/own/MicroUtils/repos/exposed/src/jvmMain/kotlin/dev/inmo/micro_utils/repos/exposed/onetomany/ExposedKeyValuesRepo.kt`
and `/home/aleksey/projects/own/MicroUtils/repos/common/.../mappers/OneToManyKeyValueMappers.kt`) exact
buildable shape:
```kotlin
package dev.inmo.wishlist.features.roles.common.repo

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.CacheRolesRepo
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.repos.kv.roles.KeyValueRolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.exposed.onetomany.ExposedKeyValuesRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/** Raw Exposed-backed `KeyValuesRepo<String, String>` — table `roles`, columns `subject`/`role`. */
fun exposedRoleSubjectToRoleRepo(database: Database) = ExposedKeyValuesRepo<String, String>(
    database = database,
    keyColumnAllocator = { text("subject") },
    valueColumnAllocator = { text("role") },
    tableName = "roles"
).withMapper<BaseRoleSubject, BaseRole, String, String>(
    // BaseRoleSubject is a *closed* @Serializable sealed interface (Direct/OtherRole, both declared
    // in kroles itself) — Kotlin's serialization plugin auto-generates its polymorphic serializer, so
    // the library's own default `kotlinx.serialization.json.Json` instance round-trips it correctly
    // with no SerializersModule registration needed (unlike this app's *open* ViewConfig hierarchy,
    // which does need explicit `polymorphic()` registration — not the same situation).
    keyFromToTo = { Json.encodeToString(BaseRoleSubject.serializer(), this) },
    keyToToFrom = { Json.decodeFromString(BaseRoleSubject.serializer(), this) },
    valueFromToTo = { plain },
    valueToToFrom = { BaseRole(this) },
)

/** kroles' own cache-backed `RolesRepo`, mirroring the exposed table per issue point 3 (already solved
 *  by the library, per round 1 §2.1.3 — no bespoke cache decorator needed). `locker` has no default in
 *  kroles' `CacheRolesRepo` constructor, so it is constructed explicitly here (not injected — matches
 *  the CRUD Repository Pattern's `CacheItemsRepo` convention of a locally-owned `SmartRWLocker()`). */
fun cachedRolesRepo(database: Database, scope: CoroutineScope): RolesRepo = CacheRolesRepo(
    originalRepo = KeyValueRolesRepo(keyValuesRepo = exposedRoleSubjectToRoleRepo(database)),
    scope = scope,
    locker = SmartRWLocker()
)
```

**`jvmMain/kotlin/JVMPlugin.kt`** — DI wiring, following the CRUD Repository Pattern's `singleWithBinds`
convention (binds kroles' own `RolesRepo`, `ReadRolesRepo`, `WriteRolesRepo` simultaneously so
read-only consumers can depend on the narrower interface):
```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithBinds<RolesRepo> {
            cachedRolesRepo(database = get(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```
`Database` is resolved via plain `get()` — already registered by `features/common/server/JVMPlugin`
(the existing, app-wide singleton every other Exposed repo in this codebase already resolves the same
way). `CoroutineScope` likewise already app-wide.

### 2.3 The feature/role aggregator + route-guard helper (Q2)

**Registry** — `roles/common/src/commonMain/kotlin/FeatureRolesRegistry.kt` (pure data, KMP-safe, no
Ktor/Exposed dependency, so it *could* in principle be read client-side too, though nothing does today):
```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * Central registry mapping a symbolic feature/capability id to the [BaseRole] required to access it.
 * Every role-gated feature registers its own requirement here (typically from its own `Plugin.setupDI`)
 * instead of hardcoding a role check inline, so "what requires what" lives in one inspectable place and
 * the route-guard helper ([dev.inmo.wishlist.features.roles.server.utils.requireRole]) has one source of
 * truth to consult. See `roles/README.md` Architecture Notes for why this registry currently has real
 * data but the guard helper has no production caller yet.
 */
object FeatureRolesRegistry {
    private val requirements = mutableMapOf<String, BaseRole>()

    /** Registers that [featureId] requires [role]. Idempotent for re-registration with the *same* role
     *  (safe to call from `setupDI`, which can run more than once in tests); throws on a conflicting
     *  re-registration (misconfiguration — two features must not silently disagree on one id's role). */
    fun register(featureId: String, role: BaseRole) {
        val existing = requirements[featureId]
        check(existing == null || existing == role) {
            "Feature '$featureId' already registered with role '${existing?.plain}', " +
                "cannot re-register with '${role.plain}'"
        }
        requirements[featureId] = role
    }

    /** Required role for [featureId], or `null` when never registered (treated as "deny" by
     *  [dev.inmo.wishlist.features.roles.server.utils.requireRole] — fail-closed on typos). */
    fun requiredRole(featureId: String): BaseRole? = requirements[featureId]
}

/** Symbolic feature ids registered against [FeatureRolesRegistry]. One `const val` per gated
 *  capability, named after the capability rather than the file/class that happens to enforce it today,
 *  so the id survives future refactors of the enforcing code. */
object RoleGatedFeatureIds {
    const val adminPanel = "admin.panel"
    const val filesAvatarChangeForOthers = "files.avatarChangeForOthers"
    const val emailSendTest = "email.sendTest"
}
```

Registered from `roles/common/commonMain/Plugin.kt.setupDI` (the one addition to the otherwise-stock
scaffolded file, §2.2):
```kotlin
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        FeatureRolesRegistry.register(RoleGatedFeatureIds.adminPanel, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.filesAvatarChangeForOthers, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.emailSendTest, SuperAdminRole)
    }
}
```
(Centralizing registration here — rather than having `admin/server`, `files/server`, `email/server`
each call `FeatureRolesRegistry.register(...)` from their own `Plugin.setupDI` — avoids giving those
three modules a new Gradle dependency on `roles/common` purely for a registration side-effect, when
point 8 already gives them a dependency on `simpleRoles/server` instead, §5. Architecture may prefer
the self-registering variant if it fits this codebase's "each feature owns its own concerns" style
better; either is acceptable, call it out as an open style choice, not a blocking question.)

**Guard helper** — `roles/server/src/commonMain/kotlin/utils/RequireRole.kt` (server module is
JVM-only/single-target per `mppJavaProject`, so `commonMain` here is effectively JVM, exactly like
`getCallerUserIdOrAnswerUnauthorized()` living under `auth/server/src/commonMain/kotlin/utils/`):
```kotlin
package dev.inmo.wishlist.features.roles.server.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Route-guard suspend function analogous to [getCallerUserIdOrAnswerUnauthorized]: resolves the caller,
 * looks up [featureId]'s required role in [FeatureRolesRegistry], and 403s when the role is missing OR
 * [featureId] was never registered (fail-closed on a typo'd id). Returns the caller's [UserId] on
 * success so callers can chain further logic, mirroring `AdminRoutingsConfigurator.requireAdmin()`'s
 * existing `UserId?` return shape.
 *
 * Called as a suspend function at the top of a route handler — this repo's existing convention (see
 * `getCallerUserIdOrAnswerUnauthorized()`), not a Ktor `Route` wrapper analogous to `authenticate { }`,
 * since role requirements are per-route-handler data (`featureId`), not a structural nesting concern.
 */
suspend fun RoutingContext.requireRole(featureId: String, rolesRepo: ReadRolesRepo): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
    val requiredRole = FeatureRolesRegistry.requiredRole(featureId) ?: run {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    val allowed = rolesRepo.contains(BaseRoleSubject.Direct(callerId.long.toString()), requiredRole)
    if (!allowed) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return callerId
}
```

**Explicit, flagged gap (not a blocking question — a design note for Architecture/Root):** per point
8's literal text, the three concrete replacements in §5 call `simpleRolesFeature.isSuperAdmin(callerId)`
directly, **not** `requireRole(...)`. `requireRole` therefore has no production call site in this
issue's scope; it is proven correct via direct unit tests against a fake `ReadRolesRepo` (§7) —
registry hit/miss, allowed/denied. This is the literal shape of what the operator asked for ("even
though only one check actually exists today, this establishes the pattern") — a real, working,
tested mechanism ready for the next role-gated feature to call, decoupled from this issue's own
narrower point-7/8 requirement. If Architecture/Root would rather have the three existing sites
actually call `requireRole(...)` (with `simpleRoles`'s `isSuperAdmin` route becoming `requireRole`'s
first real caller too, collapsing the two mechanisms), that is a reasonable alternative — flagging it
as an open implementation choice rather than deciding it unilaterally, since it changes the shape of
`simpleRoles`'s server route from "narrow one-off" to "one caller of the general guard."

### 2.4 Gradle dependencies — `roles`

`gradle/libs.versions.toml` `[versions]`: add `kroles = "0.0.2"`. `[libraries]`: add
```toml
kroles-roles = { module = "dev.inmo:kroles.roles", version.ref = "kroles" }
kroles-repos = { module = "dev.inmo:kroles.repos", version.ref = "kroles" }
```
(Coordinates verified directly against `/home/aleksey/projects/own/kroles/settings.gradle`'s project-naming rule — `rootProject.name='kroles'` + `:roles`/`:repos` → published names `kroles.roles`/`kroles.repos` — and `gradle.properties`'s `group=dev.inmo`/`version=0.0.2`. Deliberately **not** adding `kroles.repos.ktor` / `kroles.repos.ktor.client` / `kroles.repos.ktor.server` — round 1 §2.1.4's recommendation stands: that module exposes the whole role graph over HTTP with no access control, and nothing in this issue needs it.)

Also bump, in the same `[versions]` block: `microutils = "0.29.4"` → `"0.30.0"` (kroles 0.0.2 is built
against microutils 0.30.0; round 1 confirmed both versions are already resolvable from the local Gradle
cache and that the specific microutils APIs this app already uses — `initTable()`,
`ExposedKeyValueRepo`, `FullCRUDCacheRepo`, etc. — are unchanged between the two versions from reading
the sources jars; this round additionally read the *live* 0.30.0 source tree at
`/home/aleksey/projects/own/MicroUtils` directly (not just the packaged sources jar) for
`ExposedKeyValuesRepo`, `withMapper`, `WriteCRUDRepo.newObjectsFlow`, and `StandardVersionsRepo` — all
confirmed present and stable at that version). A real `./gradlew build` after the bump remains the
actual proof; flag for Verification.

`features/roles/common/build.gradle` — commonMain gains:
```groovy
api libs.kroles.roles
api libs.kroles.repos
```
(everything else — `microutils.repos.exposed` for the jvmMain Exposed impl, `microutils.coroutines`
for `SmartRWLocker`, `kotlin.serialization` for `Json` — already flows in transitively via the
scaffold's default `api project(":wishlist.features.common.common")`, per §2.2's dependency-graph
check against `features/common/common/build.gradle`.)

`features/roles/server/build.gradle` — commonMain gains, beyond the scaffold default
(`api project(":wishlist.features.roles.common")` + `api project(":wishlist.features.common.server")`):
```groovy
api project(":wishlist.features.users.common")
```
(for `UsersRepo`/`RegisteredUser`/`UserId`/`Username` types used by the bootstrap+migration in §4, and
for `getCallerUserIdOrAnswerUnauthorized` — actually that one comes from `auth/server`, so also add:)
```groovy
api project(":wishlist.features.auth.server")
```

### 2.5 `roles/server/sample.config.json` registration

Add `"dev.inmo.wishlist.features.roles.server.JVMPlugin"` to `server/sample.config.json`'s `"plugins"`
array. Per §0's correction, exact position is **not load-bearing for correctness** (unlike round 1's
assumption) — but for readability, place it after `"...users.server.JVMPlugin"` and
`"...auth.server.JVMPlugin"` (matches where a reader would expect to find a feature consuming
`UsersRepo`), e.g. immediately after `auth` and before `wishlist`.

## 3. `simpleRoles` feature (full-stack, small)

Final name: `simpleRoles`. Scaffold via `./generate_feature.sh`, register in `settings.gradle`
(3 lines, placed right after the `roles` block), register the client platform plugins normally (this
one *is* consumed by the client, unlike `roles/client`).

### 3.1 Common module

`simpleRoles/common/src/commonMain/kotlin/Constants.kt`:
```kotlin
object Constants {
    const val prefixPathPart = "simpleRoles"
    const val isSuperAdminPathPart = "isSuperAdmin"
}
```
No shared interface in `common` — server and client signatures genuinely differ (`UserId` arg vs. no
arg), which CODING.md's Full-Stack Feature pattern already anticipates ("mirror the interface if it is
not in common module of feature").

### 3.2 Server module

`simpleRoles/server/src/commonMain/kotlin/SimpleRolesFeature.kt`:
```kotlin
interface SimpleRolesFeature {
    suspend fun isSuperAdmin(userId: UserId): Boolean
}
```
`simpleRoles/server/src/commonMain/kotlin/services/SimpleRolesFeatureService.kt`:
```kotlin
class SimpleRolesFeatureService(
    private val rolesRepo: ReadRolesRepo
) : SimpleRolesFeature {
    override suspend fun isSuperAdmin(userId: UserId): Boolean =
        rolesRepo.contains(BaseRoleSubject.Direct(userId.long.toString()), SuperAdminRole)
}
```
`simpleRoles/server/src/commonMain/kotlin/configurators/SimpleRolesRoutingsConfigurator.kt`:
```kotlin
class SimpleRolesRoutingsConfigurator(
    private val feature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(Constants.prefixPathPart) {
                get(Constants.isSuperAdminPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@get
                    call.respond(feature.isSuperAdmin(callerId))
                }
            }
        }
    }
}
```
`Plugin.kt` registers `SimpleRolesFeatureService` bound to `SimpleRolesFeature`, and the routing
configurator via `singleWithRandomQualifier`, per the standard Full-Stack Feature pattern.

`simpleRoles/server/build.gradle` — commonMain: `api project(":wishlist.features.roles.common")` (for
`ReadRolesRepo`/`SuperAdminRole`), `api project(":wishlist.features.common.server")`,
`api project(":wishlist.features.auth.server")` (for `getCallerUserIdOrAnswerUnauthorized`),
`api project(":wishlist.features.users.common")` (for `UserId`).

### 3.3 Client module — the two point-10 realizations

`simpleRoles/client/src/commonMain/kotlin/SimpleRolesFeature.kt`:
```kotlin
interface SimpleRolesFeature {
    suspend fun isSuperAdmin(): Boolean
}
```

**Ktor realization** — `KtorSimpleRolesFeature.kt`, mirroring `KtorAuthFeature.isRegistrationAvailable()`
exactly (same fail-closed-on-any-error shape, verified against the live source at
`features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt:59-63`):
```kotlin
class KtorSimpleRolesFeature(private val client: HttpClient) : SimpleRolesFeature {
    private val isSuperAdminPath = "${Constants.prefixPathPart}/${Constants.isSuperAdminPathPart}"

    override suspend fun isSuperAdmin(): Boolean = runCatchingLogging {
        val response = client.get(isSuperAdminPath)
        if (!response.status.isSuccess()) return@runCatchingLogging false
        response.body<Boolean>()
    }.getOrDefault(false)
}
```
Per the "Ktor realization rule" (CODING.md), this class does HTTP only — no caching, no logic.

**Cache realization** — the concrete mechanism for point 10's "must cache only answer for is superadmin
user or not," designed by directly mirroring `features/auth/client/src/commonMain/kotlin/Plugin.kt`'s
already-working pattern for `meStateFlow` (a `MutableStateFlow` refreshed reactively off
`AuthCredentialsStorage.userAuthorised`, read at `merge(flowOf(Unit), storage.userAuthorised)`). Reusing
that exact mechanism here — keyed off the auth feature's own `meStateFlow` (a `StateFlow<RegisteredUser?>`,
already tracking login/logout **and** *which* user is logged in, not just a boolean) rather than
re-deriving from `userAuthorised` directly — means the cache also correctly re-fetches if the
authenticated identity itself changes (e.g. logout-then-different-login in the same session), not only
on a bare true/false auth-state flip:

```kotlin
// CacheSimpleRolesFeature.kt
class CacheSimpleRolesFeature(
    private val delegate: KtorSimpleRolesFeature,
    private val meState: StateFlow<RegisteredUser?>,
    scope: CoroutineScope
) : SimpleRolesFeature {
    private val cached = MutableStateFlow(false)

    init {
        merge(flowOf(Unit), meState).subscribeLoggingDropExceptions(scope) {
            cached.value = runCatchingLogging { delegate.isSuperAdmin() }.getOrDefault(false)
        }
    }

    override suspend fun isSuperAdmin(): Boolean = cached.value
}
```
Bound as the `SimpleRolesFeature` implementation in `Plugin.kt`:
```kotlin
single { KtorSimpleRolesFeature(get()) }
single { CacheSimpleRolesFeature(get(), meStateFlow, get()) }
single<SimpleRolesFeature> { get<CacheSimpleRolesFeature>() }
```
`meStateFlow` here is the `Koin.meStateFlow` accessor from `features/auth/client/Me.kt` — this makes
`simpleRoles/client` depend on `features/auth/client` (new dependency, added below), which is
architecturally sound (`ui/users` already depends on both `auth/client` and (after §5) `simpleRoles/client`,
so this is not introducing any new cross-feature coupling shape, just reusing an existing one directly
inside `simpleRoles/client` instead of leaving it to a UI-layer consumer to wire two separate flows
together itself).

**Why a concrete class, not just the interface method, for `ui/users` to consume reactively:** point
7's interface is deliberately narrow (`suspend fun isSuperAdmin(): Boolean`, no `StateFlow`) — but
`UsersModel.isCurrentUserRootFlow` (§6) needs a `StateFlow<Boolean>` for the existing reactive
`combine(...)`/`.stateIn(...)` UI wiring in `UserViewModel`/`UserEditViewModel` to keep working
unchanged. Rather than widening the `SimpleRolesFeature` interface itself (which would violate point
7's explicit "only one suspend fun" scope), `CacheSimpleRolesFeature` additionally exposes its backing
flow read-only, following the exact "Typed definition & accessor helpers" pattern CODING.md documents
for `meStateFlow` itself:
```kotlin
// in CacheSimpleRolesFeature.kt
val isSuperAdminStateFlow: StateFlow<Boolean> get() = cached.asStateFlow()
```
`ui/users/Plugin.kt` injects `CacheSimpleRolesFeature` (the concrete type, not the `SimpleRolesFeature`
interface) specifically to read `isSuperAdminStateFlow` — this is a deliberate, narrow exception to
"depend on interfaces" for exactly the same reason `Koin.meStateFlow`/`Koin.secretMeMutableStateFlow`
already exist as a public/internal pair in this codebase: the reactive flow is a *richer* capability
than the interface's single suspend method, and CODING.md's own qualifier-accessor pattern is the
sanctioned way to expose that without polluting the narrow public interface. Flag for Architecture to
confirm this is preferred over widening `SimpleRolesFeature`'s interface — it is the more conservative
reading of point 7's "only one suspend fun."

`simpleRoles/client/build.gradle` — commonMain: scaffold defaults
(`api project(":wishlist.features.simpleRoles.common")`, `api project(":wishlist.features.common.client")`)
plus `api project(":wishlist.features.auth.client")` (for `meStateFlow`) and
`api project(":wishlist.features.users.common")` (for `RegisteredUser` in the `meState` type).

## 4. `SuperAdmin` role bootstrap + `User` role auto-assignment + migration (points 5 & 6)

Supersedes round 1 §2.3 (the "3 call sites get one additional line" plan) and §2.5's ordering
assumption, per §0's correction. **Zero changes needed to `auth/server`, `AuthFeatureService.register`,
or `admin/server/UsersManagementFeature.create`** — all of point 6's "automatically to all users" is
implemented as one reactive subscription inside `roles/server` itself, using the already-existing
`WriteCRUDRepo.newObjectsFlow: Flow<RegisteredUser>` every `UsersRepo`-family repo in this codebase
already exposes (confirmed at `/home/aleksey/projects/own/MicroUtils/repos/common/.../StandartCRUDRepo.kt:104`
— "Flow that emits each newly created object after a successful `create` call" — and confirmed that
the app's bound `UsersRepo` singleton, `CacheUsersRepo`, is the one and only instance every creation
call site writes through, so subscribing to it from `roles/server` sees every creation regardless of
which feature performed it).

`roles/server/src/commonMain/kotlin/JVMPlugin.kt`:
```kotlin
object JVMPlugin : StartPlugin {
    private const val rootUsername = "root"
    private const val userRoleBackfillTableName = "users_default_role_backfill"
    private const val userRoleBackfillVersion = 1

    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.roles.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.roles.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)

        val usersRepo = koin.get<UsersRepo>()
        val rolesRepo = koin.get<RolesRepo>()
        val scope = koin.get<CoroutineScope>()
        val versionsRepo = koin.get<VersionsRepo<Database>>()

        // 1. Subscribe FIRST — before any backfill read — so a user created concurrently by another
        //    plugin's `startPlugin` (e.g. auth's root bootstrap; see §0, startPlugin is NOT ordered
        //    across plugins) is caught by this live subscription even if it races ahead of step 2's
        //    snapshot read below. `includeDirect` is idempotent, so double-granting in the overlap
        //    window between steps 1 and 2 is harmless.
        usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user ->
            grantDefaultRoles(rolesRepo, user)
        }

        // 2. One-time backfill (issue point 6's "small migration") for any user that already existed
        //    in the database before this feature's subscription could possibly have observed them
        //    (e.g. upgrading an existing deployment). Gated so it runs exactly once, ever, via the
        //    already-registered, previously-zero-callers VersionsRepo (round 1 §2.4).
        versionsRepo.setTableVersion(
            tableName = userRoleBackfillTableName,
            version = userRoleBackfillVersion,
            onUpdate = { _, _ ->
                usersRepo.getAll().values.forEach { user -> grantDefaultRoles(rolesRepo, user) }
            }
        )
    }

    private suspend fun grantDefaultRoles(rolesRepo: RolesRepo, user: RegisteredUser) {
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        rolesRepo.includeDirect(subject, UserRole)
        if (user.username.string == rootUsername) {
            rolesRepo.includeDirect(subject, SuperAdminRole)
        }
    }
}
```

This single function satisfies both points at once:
- **Point 5** ("`SuperAdmin` will be set to user `root`"): whenever a user named `root` is created
  (fresh install, via the reactive subscription) or already exists (existing deployment, via the
  migration's `getAll()` pass), it receives `SuperAdmin`. No explicit `getUserByUsername("root")`
  lookup or dependency on `auth`'s bootstrap having "already run" is needed — the check is inline on
  every created/existing user, which is simpler and race-free by construction (§0).
- **Point 6** ("automatically to all users" + "small migration to add to all currently exists users"):
  the reactive subscription is the "automatically... going forward" half; the `VersionsRepo`-gated
  `onUpdate` block is the literal "small migration" half the issue text asks for as a distinct
  mechanism.

`includeDirect` is confirmed idempotent (kroles' own `WriteRolesRepo.includeDirect` doc: "Returns
whether the state changed" — no error on redundant grants), so re-running `grantDefaultRoles` for a
user that already holds both roles (e.g. every server restart re-observing `root` isn't actually
possible here since the migration only runs once — but the *subscription* firing for the same user
twice in the overlap window, or a hypothetical future re-run, is safe regardless).

## 5. Points 8/9 — full replacement plan for the 4 existing root-check call sites

Re-audited independently this round (`grep -rn "== \"root\"\|rootUsername\|isRoot\b\|isCurrentUserRootFlow\|isRootState"`
across `features/`, plus a separate ast-index confirmation) — confirms round 1's audit is exhaustive:
exactly 3 server checks + 1 client implementation site (3 more client files consume that one site's
output and need no changes of their own). `features/ui/adminPanel/.../AdminPanelViewModel.kt` (named in
the original issue-executor's non-exhaustive lead) does **not** perform its own check — it only
documents "root-only" in a comment and relies on server-side enforcement; no code change needed there,
optional comment-wording update only.

### 5.1 `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt:68-78`

Before:
```kotlin
private val rootUsername = "root"

private suspend fun RoutingContext.requireAdmin(): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
    val user = usersRepo.getById(callerId)
    if (user == null || user.username.string != rootUsername) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return callerId
}
```
After (constructor gains `simpleRolesFeature: SimpleRolesFeature`; `usersRepo: ReadUsersRepo` is no
longer needed *by this method* — check whether it's still used elsewhere in the class before removing
the constructor parameter; it is, for `GET /admin/users/getById/{id}`, so keep the parameter):
```kotlin
private suspend fun RoutingContext.requireAdmin(): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
    if (!simpleRolesFeature.isSuperAdmin(callerId)) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return callerId
}
```
`AdminRoutingsConfigurator`'s constructor gains `simpleRolesFeature: SimpleRolesFeature` (from
`simpleRoles/server`); `admin/server/Plugin.kt`'s registration of `AdminRoutingsConfigurator(get(), ...)`
gains one more `get()`. `admin/server/build.gradle` gains
`api project(":wishlist.features.simpleRoles.server")`.

### 5.2 `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:26-60`

Before:
```kotlin
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val usersRepo: UsersRepo
) : EmailFeature {
    private val rootUsername = "root"
    ...
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        val caller = usersRepo.getById(callerId) ?: return false
        if (caller.username.string != rootUsername) return false
        return emailsService.sendText(...)
    }
```
After:
```kotlin
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val usersRepo: UsersRepo,
    private val simpleRolesFeature: SimpleRolesFeature
) : EmailFeature {
    ...
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        if (!simpleRolesFeature.isSuperAdmin(callerId)) return false
        return emailsService.sendText(...)
    }
```
Note: the existing "caller not found → false" branch collapses naturally — `isSuperAdmin` on an unknown
`UserId` resolves to `contains(...)` over an empty role set, which is `false`, same net behavior,
one fewer `usersRepo.getById` round trip for this method specifically (`usersRepo` stays a constructor
parameter — `setMyEmail` still uses it). `email/server/build.gradle` gains
`api project(":wishlist.features.simpleRoles.server")`. Update the class KDoc's "verifies [callerId] is
root" line to "verifies [callerId] is superadmin" and the `rootUsername`-mentioning line — this file
was modified very recently (issue #66/PR #69's uncommitted-at-branch-time work, per `git status` at
session start) so re-check for merge/rebase friction is a Coding-stage concern, not a Planning one.

### 5.3 `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt:46-54,113-122`

Before:
```kotlin
class FilesRoutingsConfigurator(
    private val filesService: FilesService,
    private val usersRepo: ReadUsersRepo
) : ApplicationRoutingConfigurator.Element {
    private val rootUsername = "root"

    private suspend fun isRoot(callerId: UserId): Boolean =
        usersRepo.getById(callerId)?.username?.string == rootUsername
    ...
                    if (callerId != userId && !isRoot(callerId)) {
```
After:
```kotlin
class FilesRoutingsConfigurator(
    private val filesService: FilesService,
    private val simpleRolesFeature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {
    ...
                    if (callerId != userId && !simpleRolesFeature.isSuperAdmin(callerId)) {
```
`usersRepo: ReadUsersRepo` constructor parameter is removed entirely (this was its only use in the
class — verify at Coding time that nothing else in the file reads it; from the full file read this
round, nothing else does). `files/server/build.gradle` gains
`api project(":wishlist.features.simpleRoles.server")` (and can drop nothing — it doesn't currently
depend on `users/common` directly for this purpose; `UserId` comes from elsewhere already imported in
the file).

### 5.4 Client — `features/ui/users/src/commonMain/kotlin/Plugin.kt:76-78`

Before:
```kotlin
override val isCurrentUserRootFlow: StateFlow<Boolean> =
    meState.map { it?.username?.string == "root" }
        .stateIn(scope, SharingStarted.Eagerly, meState.value?.username?.string == "root")
```
After:
```kotlin
override val isCurrentUserRootFlow: StateFlow<Boolean> = get<CacheSimpleRolesFeature>().isSuperAdminStateFlow
```
This is a straight swap of the `StateFlow<Boolean>` source — `UsersModel.isCurrentUserRootFlow`'s own
KDoc, and every downstream consumer (`UserViewModel.canEditState`, `UserEditViewModel.isRootState`/
`canSaveState`, the three platform `UserEditView.kt`s), needs **no code change**, only doc-comment
wording updates from "root" to "superadmin" where they explain *why* the flag gates access (the
`UserId`/`Boolean` shapes are unchanged). `ui/users/build.gradle` gains
`api project(":wishlist.features.simpleRoles.client")`.

Rename consideration (not required, flagged for Architecture): `isCurrentUserRootFlow`/`isRootState`
are now slightly misnamed (they gate on superadmin status, not literally "is username == root", even
though today the two are equivalent per Q1). Renaming touches every consumer listed above purely
cosmetically; Architecture can decide whether that churn is worth it in this pass or deferred — not
blocking either way since behavior is identical.

## 6. Gradle/dependency wiring — consolidated summary

| Module | New dependency |
|---|---|
| `gradle/libs.versions.toml` | `kroles = "0.0.2"` version; `kroles-roles`, `kroles-repos` libraries; bump `microutils` → `0.30.0` |
| `features/roles/common/build.gradle` | `libs.kroles.roles`, `libs.kroles.repos` |
| `features/roles/server/build.gradle` | `:wishlist.features.users.common`, `:wishlist.features.auth.server` (beyond scaffold defaults) |
| `features/simpleRoles/server/build.gradle` | `:wishlist.features.roles.common`, `:wishlist.features.common.server`, `:wishlist.features.auth.server`, `:wishlist.features.users.common` |
| `features/simpleRoles/client/build.gradle` | `:wishlist.features.auth.client`, `:wishlist.features.users.common` (beyond scaffold defaults) |
| `features/admin/server/build.gradle` | `:wishlist.features.simpleRoles.server` |
| `features/email/server/build.gradle` | `:wishlist.features.simpleRoles.server` |
| `features/files/server/build.gradle` | `:wishlist.features.simpleRoles.server` |
| `features/ui/users/build.gradle` | `:wishlist.features.simpleRoles.client` |
| `client/build.gradle`, `client/src/jsMain/.../Main.kt`, `client/src/jvmMain/.../Main.kt`, `client/android/.../MainActivity.kt` | register `simpleRoles.client`'s platform plugins normally (standard Feature Adding Rules steps 3/5) |
| `server/sample.config.json` | add `roles.server.JVMPlugin`; `simpleRoles` has no separate server plugin registration beyond what its own scaffold JVMPlugin needs — confirm at Coding time whether `simpleRoles/server` needs its own `sample.config.json` entry (yes — it registers its own routing configurator via `singleWithRandomQualifier` in its own `Plugin.setupDI`, so it needs the standard entry like every other full-stack feature) |
| `settings.gradle` | `:features:roles:{common,server,client}`, `:features:simpleRoles:{common,server,client}` |

`roles/client` is **not** wired anywhere beyond `settings.gradle` (§2.1's deliberate deviation).

## 7. Test stub candidates for Architecture to formalize

Mirrors the just-completed `EmailFeatureService`/`EmailsService` task's test structure on this repo
(`kotlin.test` + `kotlinx-coroutines-test`'s `runTest`, small `Fake*Repo` doubles backed by
`MapCRUDRepo`/plain in-memory maps, no live-DB tests anywhere in this codebase — confirmed via
`grep -rln "Database.connect|H2|testcontainers"` returning nothing; Exposed-touching code is verified
via full build + manual run, per this repo's established convention, not unit tests).

**`SimpleRolesFeatureServiceTest`** (`simpleRoles/server/src/commonTest`):
- `isSuperAdmin` returns `true` for a subject with `SuperAdminRole` directly granted (fake `ReadRolesRepo`
  seeded with `Direct("1") -> [SuperAdminRole]`).
- `isSuperAdmin` returns `false` for a subject with only `UserRole` (no `SuperAdminRole`).
- `isSuperAdmin` returns `false` for a subject with no roles at all (unknown/never-granted `UserId`).
- Needs a small `FakeRolesRepo : ReadRolesRepo` test double (`contains`/`getDirectRoles`/etc. backed by
  a `Map<BaseRoleSubject, Set<BaseRole>>`) — new fixture, no existing one to reuse.

**`RolesServerJVMPluginTest`** (or split into two, Architecture's call) — the reactive-subscription +
migration logic from §4, isolated from Koin/Ktor (a plain suspend function taking `UsersRepo`,
`RolesRepo`, `VersionsRepo<Database>` fakes, extracted from the `JVMPlugin` object so it's callable
without booting the whole app — Architecture should structure `grantDefaultRoles`/the migration body as
a standalone, testable class or top-level function rather than inline in `JVMPlugin.startPlugin`, per
this codebase's own precedent of `emailConfigElementOrNull` being pulled out of `Plugin.kt` specifically
so `PluginTest.kt` can exercise it without a Koin harness):
- Backfill: given a `FakeUsersRepo` seeded with N pre-existing users (one named `root`, others not),
  running the migration body once grants `UserRole` to all N and `SuperAdminRole` only to `root`.
  Running it a second time (simulating a second `setTableVersion` call, or directly re-invoking the
  extracted body) is a no-op / does not throw / does not double-grant in a way that changes semantics
  (`includeDirect` idempotency).
- Backfill is version-gated: using a real (or fake) `VersionsRepo`, confirm `setTableVersion` with a
  stored version already `>= 1` does **not** re-invoke the `onUpdate` body (this exercises
  `StandardVersionsRepo`'s own already-library-level behavior — a thin integration-shaped test, or skip
  if Architecture judges it purely tests library code with no app-specific risk).
- Reactive path: given a fake `newObjectsFlow` (a `MutableSharedFlow<RegisteredUser>` a test can emit
  into), emitting a non-root user grants only `UserRole`; emitting a user named `root` grants both
  `UserRole` and `SuperAdminRole`.

**`FeatureRolesRegistryTest`** (`roles/common/src/commonTest`):
- `register` then `requiredRole` round-trips the same `BaseRole`.
- Re-registering the same `featureId` with the same `BaseRole` does not throw.
- Re-registering the same `featureId` with a *different* `BaseRole` throws (`IllegalStateException` via
  `check`).
- `requiredRole` for a never-registered id returns `null`.

**`RequireRoleTest`** (`roles/server/src/commonTest`) — needs a minimal fake `RoutingContext`/`call`
harness; check whether this repo has any existing precedent for unit-testing a `RoutingContext`
extension directly (a quick search this round found none — `AuthRoutingsConfigurator`,
`AdminRoutingsConfigurator` etc. have no direct unit tests, only the pure-logic helpers around them do).
If no lightweight way to construct a fake `RoutingContext` exists, Architecture should either (a) extract
the pure decision logic (`registry lookup → role → contains check → allow/deny`) into a small,
`RoutingContext`-free function that `requireRole` thinvely wraps, and unit-test *that* instead (matching
this repo's established "extract the pure part for testability" convention, e.g. `isUniqueViolation`,
`emailConfigElementOrNull`), or (b) flag the Ktor-coupled parts as covered by build+manual verification
only, per the Test Planning Requirement's explicit allowance for untestable-through-automation surfaces
— **recommend (a)**, it costs little and keeps the guard's actual allow/deny decision under test:
  - allowed when the registry has `featureId -> role` and the subject has that role.
  - denied (without calling `rolesRepo` unnecessarily, if practical) when `featureId` is unregistered.
  - denied when the subject lacks the required role.

**`AdminRoutingsConfiguratorTest` / `EmailFeatureServiceTest` (extended) / `FilesRoutingsConfiguratorTest`**
— for the 3 rewritten call sites, at minimum: a fake `SimpleRolesFeature` returning `true` unlocks the
gated behavior, returning `false` produces the same `403`/`false` outcome the old root-username check
produced. `EmailFeatureServiceTest` already exists and has direct root/non-root/not-found cases (§ per
round 1's own reference) — those three test methods need to be re-pointed at a `FakeSimpleRolesFeature`
instead of a `FakeUsersRepo`-based root/non-root fixture; `AdminRoutingsConfiguratorTest` and
`FilesRoutingsConfiguratorTest` (routing-configurator-level tests) do not currently exist in this
codebase for these two files (only `email/server` has route-adjacent tests) — Architecture should decide
whether to add them net-new here or rely on the service/repo-level tests above plus manual/build
verification, consistent with this repo's actual existing test-coverage depth (routing configurators
elsewhere are not unit-tested directly; the logic they call is).

**Exposed-mapper round-trip test** (`roles/common/src/commonTest` or `jvmTest`) — the
`BaseRoleSubject <-> String` and `BaseRole <-> String` mapper lambdas from §2.2 are pure functions and
should be extracted (or tested via the `withMapper` lambdas directly, if Architecture keeps them
inline) independent of any live Exposed connection, per this repo's no-live-DB-tests convention:
- `Json.encodeToString(BaseRoleSubject.serializer(), Direct("1"))` then decoding back yields an
  equal `Direct("1")` (not an `OtherRole`).
- Same round-trip for `OtherRole(SuperAdminRole)`.
- `BaseRole.plain` / `BaseRole(string)` round-trips trivially (already covered implicitly by the above
  if the mapper is tested as one unit, otherwise a one-line dedicated test is cheap).

## 8. README updates needed

**New READMEs** (mandatory per `agents/ALL.md`'s Feature README Rule — both created empty-Operator-Notes,
since neither is a case where the operator has pre-written constraints beyond what's already captured in
`features/admin/README.md`'s existing Operator Notes, which are untouched):

- `features/roles/README.md` — Overview (server-only role storage feature wrapping `dev.inmo:kroles`);
  no `## Routes` table (no HTTP surface — note this explicitly rather than leaving the section
  template-empty and ambiguous); Models section documents `SuperAdminRole`/`UserRole` constants,
  `FeatureRolesRegistry`/`RoleGatedFeatureIds`, the Exposed table (`roles`, columns `subject`/`role`);
  Architecture Notes documents: the `roles/client` deliberate-stub decision (§2.1), the
  subscribe-then-backfill race-avoidance design (§0/§4) and *why* (link back to the concurrent-`startPlugin`
  fact), the registry-has-data-but-guard-has-no-production-caller gap (§2.3), the `withMapper`-based
  Exposed adapter design (§2.2), and the explicit "do not add `kroles.repos.ktor.*`" note.
- `features/simpleRoles/README.md` — Overview (narrow "am I superadmin" full-stack feature); Routes
  table (`GET /simpleRoles/isSuperAdmin`, bearer auth, → `Boolean`); Models (`SimpleRolesFeature`
  server/client signatures, explicitly noting they're not mirrored 1:1 and why); Architecture Notes
  documents the Ktor+Cache client split (§3.3), the `isSuperAdminStateFlow` concrete-type-injection
  exception to "depend on interfaces" and why, and the `meStateFlow`-keyed cache-invalidation design.

**Updated READMEs** (existing consumer features, per ARCHITECTURE.md's "AFTER ANY CODE CHANGE" rule —
Architecture Notes only, Operator Notes untouched in every case):

- `features/admin/README.md` — Architecture Notes: `requireAdmin()` now delegates to
  `SimpleRolesFeature.isSuperAdmin`; still 403s non-superadmins; note the constructor no longer needs
  `usersRepo` for this check (kept for the unrelated `getById` route).
- `features/email/README.md` — Architecture Notes ("Root guard" bullet, §55 of the current file):
  update to describe the `simpleRoles`-backed check replacing the inline username comparison; the "caller
  not found → false" collapse noted in §5.2.
- `features/files/README.md` — Architecture Notes (the avatar-authorization bullet, §47 of the current
  file): update "root resolved via `ReadUsersRepo`... username `"root"`" to describe the
  `SimpleRolesFeature.isSuperAdmin` delegation; note `ReadUsersRepo` constructor param removal.
- `features/ui/users/README.md` — Architecture Notes: the "Root detection is client-side
  (`me.value?.username?.string == "root"`)" line (currently accurate, will become stale) needs to
  describe the new `CacheSimpleRolesFeature.isSuperAdminStateFlow`-backed detection instead; note the
  server still enforces independently (unchanged fact, just re-worded mechanism).
- `features/users/README.md` — no functional change from this issue, but its "Root user bootstrap
  happens inside `auth/server/JVMPlugin.startPlugin`" line remains accurate (bootstrap itself is
  untouched by §4's design — only the *role-granting* is new and lives in `roles/server`, not here); a
  one-line cross-reference to `roles/README.md` for "which roles get granted to a newly created user and
  by what mechanism" is a reasonable, optional addition.
- `features/auth/README.md` — no functional change; optional cross-reference only, same rationale as
  `users/README.md`.

## 9. Coding split proposal

Given the size (2 new feature modules, a Gradle version-catalog bump touching every JVM Exposed
consumer transitively, a reactive cross-cutting subscription, 4 call-site rewrites across 4 already-
existing features, 2 new READMEs + 6 updated ones), **yes, this warrants a multi-pass Coding split**,
mirroring how issue #67 was split. Proposed passes, each independently buildable/committable:

1. **Foundation**: Gradle version-catalog changes (`kroles` + `microutils` bump), scaffold `roles` +
   `simpleRoles` (all 6 submodules, `settings.gradle`), `roles/common`'s model+repo+DI layer (§2.2),
   `simpleRoles`'s full server+client implementation (§3) — **no call sites touched yet**, nothing in
   the rest of the app changed. Verify: full build green, `roles`+`simpleRoles` unit tests green,
   `simpleRoles`'s route independently curl-able returning `false` for any real user (nobody has
   `SuperAdmin` yet since pass 2 hasn't run).
2. **Bootstrap + migration**: `roles/server/JVMPlugin`'s reactive subscription + `VersionsRepo`-gated
   backfill (§4), `sample.config.json` registration. Verify: fresh-DB boot grants `root` both roles;
   existing-DB boot (simulate by running twice) backfills `User` to pre-existing users without
   duplicating grants; unit tests from §7's "Reactive path"/"Backfill" bullets green.
3. **Aggregator + guard**: `FeatureRolesRegistry`/`RoleGatedFeatureIds`/`requireRole` (§2.3) — purely
   additive, zero existing call sites touched yet, unit-tested per §7. Could be folded into pass 1 if
   Architecture prefers fewer passes; kept separate here because it's conceptually distinct
   (cross-feature policy infrastructure vs. this feature's own storage layer) and because its "no
   production caller" gap (§2.3) is easier to review in isolation.
4. **Call-site replacement**: the 3 server + 1 client rewrites (§5), the 4 consumer `build.gradle`
   additions, updated existing tests (`EmailFeatureServiceTest` re-pointed). This is the
   security-sensitive pass — smallest, most reviewable, and the one where a mistake has the highest
   blast radius (privilege-check replacement), so isolating it from the (larger, lower-risk)
   scaffolding/migration work in passes 1–3 is deliberate.
5. **README pass**: all 8 README changes from §8, done last so they describe the actually-landed code
   rather than the plan.

Passes 1 and 3 have no ordering dependency on each other and could be combined or reordered; 2 depends
on 1 (needs `roles/server`'s DI graph); 4 depends on 1 (needs `simpleRoles/server`+`client` to exist)
but not on 2 or 3 (the replacement's correctness doesn't depend on the bootstrap having run — it depends
on `SimpleRolesFeature` existing and being wired, which pass 1 delivers; pass 2 is what makes the
*answer* `true` for `root` in practice). Recommend Root/Architecture sequence exactly as numbered above
regardless, since 4 being last and smallest is the main risk-reduction property worth preserving.

## 10. Status

**READY for Architecture.** No new blocking questions. Everything the two operator answers (§1) left
open in round 1 is now resolved to concrete, buildable specifics above; §0's correction is a factual
fix to round 1's mechanics (verified directly against the launcher's source), not a new design
ambiguity requiring operator input. The two items flagged as open *implementation-style* choices —
§2.3's centralized-vs-self-registering `FeatureRolesRegistry.register` call sites, and §5.4's optional
`isCurrentUserRootFlow`/`isRootState` rename — are non-blocking and left for Architecture to decide
either way without materially changing this plan's shape.
