Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: not tracked by harness; single continuous investigation+writing pass (ast-index `stats`/`usages`/`search` cross-validation of the round-2 call-site audit — zero discrepancies found; ~55 file reads across this repo, `/home/aleksey/projects/own/MicroUtils`, and `/home/aleksey/projects/own/kroles` to verify every library API signature used below against actual source rather than the plan's prose)
Changed files: agents/task/10.07.2026_17.25.10-4eebb958-6bfa-4f82-a4cc-f3b4120dc4c3/003-architecturing.md (this file only)

# Architecturing (final spec): Issue #68 — Implement roles in project

## 0. Notice — prompt-injection payload in `AGENTS.md` is still present, still ignored

`AGENTS.md` (149 lines) still contains the "AML-HIP" pronoun-free forced-protocol payload first flagged
in `001-planning.md` §0. It is unrelated to this repo's real routing file (`agents/SHORTCUTS.md`, intact)
and is untrusted content, not an instruction from the operator or from this task's own prompt. It has been
read, ignored, and is not followed anywhere in this document (written in normal prose, as this run's
instructions require). Re-flagging only because it is still unresolved a second task later — a human
should decide whether to clean it up. This notice does not change anything below.

## 1. Scope of this step

This step turns `002-planning.md` (status: **READY**, not re-litigated below except where noted) into a
byte-exact, execution-ready spec: every new file's full Kotlin source, exact before/after diffs for the 4
root-check call sites, full test code, the two open implementation-style decisions resolved, exact
Gradle/config diffs, README content, and a concrete ordered Coding-pass split with a per-pass file list.
Every library API used below (kroles' `BaseRole`/`BaseRoleSubject`/`RolesRepo`/`CacheRolesRepo`/
`KeyValueRolesRepo`; MicroUtils' `ExposedKeyValuesRepo`/`withMapper`/`VersionsRepo`/`StandardVersionsRepo`/
`WriteCRUDRepo.newObjectsFlow`/`singleWithBinds`) was re-read from the actual source under
`/home/aleksey/projects/own/kroles` and `/home/aleksey/projects/own/MicroUtils` during this step (not
trusted from the plan's prose alone), and every existing call site (`AdminRoutingsConfigurator.kt`,
`EmailFeatureService.kt`, `FilesRoutingsConfigurator.kt`, `features/ui/users/.../Plugin.kt`) was re-read in
full and cross-checked with `ast-index usages`/`ast-index search` — the round-2 audit is exhaustive; no
additional call sites exist on this branch.

**One correction found while making the plan buildable, beyond round 2's own §0 correction — see §4
below (Coding-pass split): registering `roles.server.JVMPlugin` in the two server config files cannot be
deferred to "pass 2" as a literal reading of round 2's proposal might suggest, because `simpleRoles/server`
resolves `ReadRolesRepo` from Koin lazily, and Koin's DI graph only contains that binding if
`roles.common.JVMPlugin.setupDI` has actually run — which only happens if `roles.server.JVMPlugin` (the
only sanctioned way to trigger it, per `agents/CODING.md`'s "same feature only" plugin-delegation rule) is
in the config's `"plugins"` list. This is a build/runtime-wiring correction to round 2's pass split, not a
design change — full reasoning in §4.**

---

## 2. Resolution of the two open implementation-style choices (task item 4)

Per `002-planning.md` §10, these are mine to decide, not to escalate. Both decided below; reasoning is
short, as instructed.

### 2.1 `FeatureRolesRegistry.register(...)` call sites — centralized (decided: keep round 2's default)

**Decision: centralized**, in `roles/common/Plugin.kt` (the shape round 2 already sketched as its primary
proposal), not self-registered by `admin/server`/`files/server`/`email/server`'s own `Plugin.setupDI`.

**Reasoning:** the registry has zero production consultation today — the three concrete point-8
replacements (§5 below) call `SimpleRolesFeature.isSuperAdmin(...)` directly, not `requireRole(...)`, per
the issue's own literal text (see §2.2 below). Self-registering would force `admin/server`, `files/server`,
and `email/server` — three already-stable, otherwise-unrelated features — to each take on a *new* Gradle
dependency on `roles/common` purely for a side-effecting registration call that nothing in their own code
consults. That dependency would inflate the diff of Coding pass 4 (§4), which is deliberately the
smallest, most reviewable pass because it is the security-sensitive one (replacing privilege checks). A
future feature that actually calls `requireRole(...)` for its own gating should self-register at that
point — self-registration is the right pattern for a real caller, not for a still-hypothetical one.

### 2.2 `isCurrentUserRootFlow` / `isRootState` rename — decided: keep the names, update only doc comments

**Decision: do not rename.** `UsersModel.isCurrentUserRootFlow`, `UserEditViewModel.isRootState`, and every
consumer (`UserViewModel.canEditState`, `UserEditViewModel.canSaveState`, the three platform
`UserEditView.kt`s) keep their current identifiers. Only the KDoc/comment prose that explains *why* the
flag gates access is updated (guidance only — see the end of §5.4; not a mandatory byte-exact diff since
behavior and property names are unchanged).

**Reasoning:** behavior is unchanged (Q1 locked `SuperAdmin` to `root`-only, permanently, so
"is-root" and "is-superadmin" remain the exact same boolean forever), so a rename is purely cosmetic and
would touch 6 files (`Plugin.kt` + 5 consumers) for zero functional gain — pure diff-size/review-risk
added to the same security-sensitive pass with no safety benefit. `agents/CODING.md` does not mandate
this rename, and the plan itself flagged it as optional. Keeping the names also keeps Coding pass 4's diff
to exactly what's functionally required: the `StateFlow<Boolean>`'s *source* changes; its name, type, and
every downstream consumer's code stays untouched.

*(Note, not one of "the two": round 2 §2.3 also floated whether the 3 point-8 replacements should call
`requireRole(...)` instead of `SimpleRolesFeature.isSuperAdmin(...)` directly, collapsing the two
mechanisms. Round 2 §1 already settled this as intentional ("this divergence... is intentional and
flagged explicitly, not smoothed over") and did not list it among "the two" open choices in its final
Status section — so it is carried forward as-is, not re-decided here: the 3 sites call
`SimpleRolesFeature.isSuperAdmin(...)` directly, per point 8's literal text, and `requireRole` remains a
real, tested, currently-callerless guard for the next role-gated route.)*

---

## 3. New files — byte-exact Kotlin, Gradle, and config

All package/module names below use the plan's final names: feature `roles` (server-only in practice,
`roles/client` a deliberate permanent stub) and feature `simpleRoles` (full-stack, small). Every "stock"
file below is the literal, unmodified output of `./generate_feature.sh` (verified against
`.templates/standard_module_kts/{{$module_path}}/...` — read directly this step, not assumed) with
`{{$module_path}}` substituted; these are listed for completeness of the per-pass file inventory (§4) even
though their content requires no manual authoring.

### 3.1 Gradle version catalog — `gradle/libs.versions.toml`

**`[versions]`** — change `microutils = "0.29.4"` → `"0.30.0"` (kroles 0.0.2 is built against microutils
0.30.0; already resolvable from the local Gradle cache per round 1/2's confirmation) and add `kroles`,
placed next to the other InsanusMokrassar entries:

```diff
 krontab = "2.9.0"
 navigation = "0.7.5"
-microutils = "0.29.4"
+microutils = "0.30.0"
+kroles = "0.0.2"
 compose = "1.11.0"
 kslog = "1.6.1"
```

**`[libraries]`** — add two entries right after the last `microutils-*` line (`microutils-transactions`),
before the blank line + `# buildscript classpaths` section:

```diff
 microutils-serialization-typedserializer = { module = "dev.inmo:micro_utils.serialization.typed_serializer", version.ref = "microutils" }
 microutils-serialization-base64 = { module = "dev.inmo:micro_utils.serialization.base64", version.ref = "microutils" }
 microutils-startup-plugin = { module = "dev.inmo:micro_utils.startup.plugin", version.ref = "microutils" }
 microutils-startup-launcher = { module = "dev.inmo:micro_utils.startup.launcher", version.ref = "microutils" }
 microutils-selector = { module = "dev.inmo:micro_utils.selector.common", version.ref = "microutils" }
 microutils-resources = { module = "dev.inmo:micro_utils.resources", version.ref = "microutils" }
 microutils-colors-common = { module = "dev.inmo:micro_utils.colors.common", version.ref = "microutils" }
 microutils-transactions = { module = "dev.inmo:micro_utils.transactions", version.ref = "microutils" }
+
+kroles-roles = { module = "dev.inmo:kroles.roles", version.ref = "kroles" }
+kroles-repos = { module = "dev.inmo:kroles.repos", version.ref = "kroles" }
```

Deliberately **not** adding `kroles.repos.ktor` / `kroles.repos.ktor.client` / `kroles.repos.ktor.server` —
that module exposes kroles' entire generic role graph (create/remove roles, include/exclude on any
subject, full read) over HTTP with no access control of its own; nothing in issue #68 needs it, and using
it would contradict point 7/10's explicit "only one narrow check" scope. Coordinates verified directly
against `/home/aleksey/projects/own/kroles/settings.gradle` (`rootProject.name='kroles'`, subprojects
`:roles`/`:repos` → published artifact ids `kroles.roles`/`kroles.repos`) and `gradle.properties`
(`group=dev.inmo`, `version=0.0.2`).

### 3.2 `settings.gradle`

Insert after the `":features:deeplinks:client"` block (currently ends at line 46) and before the
`":features:ui:sample"` block (currently line 48), matching the file's existing blank-line grouping:

```diff
     ":features:deeplinks:common",
     ":features:deeplinks:server",
     ":features:deeplinks:client",
 
+    ":features:roles:common",
+    ":features:roles:server",
+    ":features:roles:client",
+
+    ":features:simpleRoles:common",
+    ":features:simpleRoles:server",
+    ":features:simpleRoles:client",
+
     ":features:ui:sample",
     ":features:ui:auth",
```

### 3.3 `features/roles/common` (KMP: JVM+JS+Android via `mppJvmJsAndroid`)

**`build.gradle`** (scaffold default + kroles deps):

```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
    id "com.android.library"
}

apply from: "$mppJvmJsAndroid"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.common.common")
                api libs.kroles.roles
                api libs.kroles.repos
            }
        }
    }
}
```

`microutils.repos.exposed` (jvmMain), `microutils.coroutines` (for `SmartRWLocker`), and
`kotlinx-serialization-json` (for `Json` in the mapper) all already flow in transitively —
`features/common/common/build.gradle` (read this step) declares `api libs.microutils.repos.exposed` in its
own `jvmMain` block and `api libs.microutils.coroutines` in `commonMain`; `kotlinx-serialization-json` is
added by the `defaultProjectWithSerialization` template that `mppJvmJsAndroid` itself applies.

**`src/commonMain/kotlin/Constants.kt`** — stock scaffold, left exactly as generated (single package
line, no content — this feature has no path constants, no HTTP surface):

```kotlin
package dev.inmo.wishlist.features.roles.common
```

**`src/commonMain/kotlin/RoleConstants.kt`** (new):

```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * The single, hardcoded, root-only administrative role (issue #68 point 5). Never grantable through
 * any UI/route in this app — SuperAdmin access is architecturally fixed to the `root` account; see
 * `roles/README.md` Architecture Notes and `features/admin/README.md` Operator Notes.
 */
val SuperAdminRole = BaseRole("SuperAdmin")

/**
 * The single, hardcoded role every registered user holds (issue #68 point 6). Granted automatically
 * on user creation and backfilled once for pre-existing users — see
 * [dev.inmo.wishlist.features.roles.server.JVMPlugin].
 */
val UserRole = BaseRole("User")
```

**`src/commonMain/kotlin/FeatureRolesRegistry.kt`** (new — the point-4 aggregator; per §2.1, registration
of today's real data is centralized in `Plugin.kt` below, not self-registered by consumer features):

```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * Central registry mapping a symbolic feature/capability id to the [BaseRole] required to access it
 * (issue #68 point 4's "aggregator of features and required role for them"). Every role-gated
 * capability's requirement is recorded here — currently populated from [Plugin.setupDI] — so "what
 * requires what" lives in one inspectable place, consulted by the route-guard helper
 * ([dev.inmo.wishlist.features.roles.server.utils.requireRole]). See `roles/README.md` Architecture
 * Notes for why this registry currently has real data but the guard helper has no production caller
 * yet — the three concrete privilege-check replacements this issue makes (`admin`, `email`, `files`)
 * call `SimpleRolesFeature.isSuperAdmin` directly instead, per the issue's own literal text.
 */
object FeatureRolesRegistry {
    private val requirements = mutableMapOf<String, BaseRole>()

    /**
     * Registers that [featureId] requires [role]. Idempotent for re-registration with the *same*
     * role (safe to call more than once, e.g. from a `setupDI` that could run more than once in
     * tests); throws on a conflicting re-registration — two features must never silently disagree on
     * one id's required role.
     *
     * @param featureId Symbolic id of the gated capability (see [RoleGatedFeatureIds]).
     * @param role Role required to access [featureId].
     * @throws IllegalStateException when [featureId] is already registered with a different role.
     */
    fun register(featureId: String, role: BaseRole) {
        val existing = requirements[featureId]
        check(existing == null || existing == role) {
            "Feature '$featureId' already registered with role '${existing?.plain}', " +
                "cannot re-register with '${role.plain}'"
        }
        requirements[featureId] = role
    }

    /**
     * Looks up the role required to access [featureId].
     *
     * @param featureId Symbolic id previously passed to [register].
     * @return The required [BaseRole], or `null` when [featureId] was never registered — treated as
     *   "deny" by [dev.inmo.wishlist.features.roles.server.utils.requireRole] (fail-closed on a typo).
     */
    fun requiredRole(featureId: String): BaseRole? = requirements[featureId]
}

/**
 * Symbolic feature ids registered against [FeatureRolesRegistry]. One `const val` per gated
 * capability, named after the capability rather than the file/class that happens to enforce it today,
 * so the id survives future refactors of the enforcing code.
 */
object RoleGatedFeatureIds {
    /** The whole `/admin/...` route surface (`AdminRoutingsConfigurator`). */
    const val adminPanel = "admin.panel"

    /** Changing another user's avatar via `PUT /files/avatar/{userId}` (`FilesRoutingsConfigurator`). */
    const val filesAvatarChangeForOthers = "files.avatarChangeForOthers"

    /** Sending a test email via `POST /email/sendTest` (`EmailFeatureService.sendTestEmail`). */
    const val emailSendTest = "email.sendTest"
}
```

**`src/commonMain/kotlin/Plugin.kt`** (populated — see §4 for why this is written now even though it is
implemented in Coding pass 3, not pass 1):

```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common startup plugin for the `roles` feature.
 *
 * Registers no Koin dependencies of its own — the Exposed/cache [dev.inmo.kroles.repos.RolesRepo]
 * wiring lives in [dev.inmo.wishlist.features.roles.common.JVMPlugin] (JVM-only, since the backing
 * store is Exposed/JDBC). This object's only responsibility is populating [FeatureRolesRegistry] with
 * the feature/role requirements this app currently has (see `roles/README.md` Architecture Notes for
 * why registration is centralized here rather than self-registered by each gated feature).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        FeatureRolesRegistry.register(RoleGatedFeatureIds.adminPanel, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.filesAvatarChangeForOthers, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.emailSendTest, SuperAdminRole)
    }
}
```

**`src/jvmMain/kotlin/repo/RolesRepoFactory.kt`** (new — verified byte-for-byte against
`/home/aleksey/projects/own/MicroUtils/repos/exposed/src/jvmMain/kotlin/.../onetomany/ExposedKeyValuesRepo.kt`,
`.../repos/common/src/commonMain/kotlin/.../mappers/OneToManyKeyValueMappers.kt`, and
`/home/aleksey/projects/own/kroles/repos/src/commonMain/kotlin/{CacheRolesRepo,kv/roles/KeyValueRolesRepo}.kt`
this step):

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

/**
 * Builds the raw Exposed-backed `KeyValuesRepo<BaseRoleSubject, BaseRole>` used by [cachedRolesRepo].
 *
 * Persists to a `roles` table with two text columns: `subject` holds the JSON-serialized
 * [BaseRoleSubject] (round-trips the sealed `Direct`/`OtherRole` variants — the plain
 * [BaseRoleSubject.rawValue] string alone would not disambiguate them), `role` holds the plain
 * [BaseRole.plain] string directly (unambiguous, since [BaseRole] has only one shape). Wrapped via
 * the MicroUtils `withMapper` adapter rather than a hand-rolled repo class, mirroring how
 * `ExposedPasswordsRepo` wraps `ExposedKeyValueRepo<String, String>` for `features/auth/server`.
 *
 * @param database Shared app-wide Exposed [Database], registered by `features/common/server`.
 * @return A `KeyValuesRepo<BaseRoleSubject, BaseRole>` backed by the `roles` table.
 */
fun exposedRoleSubjectToRoleRepo(database: Database) = ExposedKeyValuesRepo<String, String>(
    database = database,
    keyColumnAllocator = { text("subject") },
    valueColumnAllocator = { text("role") },
    tableName = "roles"
).withMapper<BaseRoleSubject, BaseRole, String, String>(
    keyFromToTo = { Json.encodeToString(BaseRoleSubject.serializer(), this) },
    keyToToFrom = { Json.decodeFromString(BaseRoleSubject.serializer(), this) },
    valueFromToTo = { plain },
    valueToToFrom = { BaseRole(this) },
)

/**
 * Builds the app's single [RolesRepo]: kroles' own [CacheRolesRepo] wrapping kroles' own
 * [KeyValueRolesRepo] wrapping [exposedRoleSubjectToRoleRepo]. Per issue #68 point 3, the cache fully
 * mirrors the Exposed-backed state — this is already solved by kroles' library-level [CacheRolesRepo]
 * (rebuilds its in-memory snapshot on every underlying change flow), so no bespoke cache decorator is
 * written here.
 *
 * @param database Shared app-wide Exposed [Database].
 * @param scope Shared app-wide [CoroutineScope] driving the cache's background refresh job.
 * @return A cache-backed [RolesRepo] ready to bind into Koin.
 */
fun cachedRolesRepo(database: Database, scope: CoroutineScope): RolesRepo = CacheRolesRepo(
    originalRepo = KeyValueRolesRepo(keyValuesRepo = exposedRoleSubjectToRoleRepo(database)),
    scope = scope,
    locker = SmartRWLocker()
)
```

**`src/jvmMain/kotlin/JVMPlugin.kt`** (customized — DI wiring, `singleWithBinds` per the CRUD Repository
Pattern's convention):

```kotlin
package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.roles.common.repo.cachedRolesRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM startup plugin for the `roles` feature.
 *
 * Registers the Exposed-backed, cache-mirrored [RolesRepo] (see
 * [dev.inmo.wishlist.features.roles.common.repo.cachedRolesRepo]) as [RolesRepo],
 * [dev.inmo.kroles.repos.ReadRolesRepo], and [dev.inmo.kroles.repos.WriteRolesRepo] simultaneously,
 * mirroring this codebase's CRUD Repository Pattern DI wiring (`agents/CODING.md`). The shared
 * `Database`/`CoroutineScope` singletons (registered by `features/common/server/JVMPlugin`) are
 * resolved by plain `get()`, not by delegating to that plugin's `setupDI` — per `agents/CODING.md`'s
 * rule that a plugin may only call `setupDI`/`startPlugin` of a plugin within its own feature.
 */
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

**`src/jsMain/kotlin/JSPlugin.kt`** and **`src/androidMain/kotlin/AndroidPlugin.kt`** — stock scaffold,
unmodified (substitute `{{$module_package}}` → `features.roles.common`); never wired into any client entry
point (§3.5 explains why).

### 3.4 `features/roles/server` (JVM-only, `mppJavaProject`)

**`build.gradle`** — **pass 1** ships the unmodified scaffold default (only `roles.common` +
`common.server`); **pass 2** adds `users.common` (for `UsersRepo`/`RegisteredUser`) and `auth.server` (for
`getCallerUserIdOrAnswerUnauthorized`, used later by `RequireRole.kt` in pass 3 — already present from
pass 2, no further Gradle change needed in pass 3):

```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
}

apply from: "$mppJavaProject"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.roles.common")
                api project(":wishlist.features.common.server")
                api project(":wishlist.features.users.common")
                api project(":wishlist.features.auth.server")
            }
        }
    }
}
```

(Pass 1's version of this file omits the last two lines; pass 2 adds them — see §4.)

**`src/commonMain/kotlin/Plugin.kt`** — stock scaffold, unmodified for the whole task (all real logic is
JVM-specific and lives in `jvmMain`):

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
```

**`src/commonMain/kotlin/RolesBootstrap.kt`** (new, **pass 2** — extracted as pure, Koin/`VersionsRepo`/
`Database`-free functions specifically so both are directly unit-testable without a Koin/Exposed harness,
mirroring this repo's existing `emailConfigElementOrNull` precedent):

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo

/** Username of the single, hardcoded SuperAdmin recipient (issue #68 point 5). */
internal const val rootUsername = "root"

/**
 * Grants the User role to [user] and, when [user] is the `root` account, additionally grants the
 * SuperAdmin role. Idempotent — kroles' `RolesRepo.includeDirect` is a no-op (returns `false`, no
 * error) when the subject already holds the role — so this is safe to call more than once for the
 * same user (see `roles/README.md` Architecture Notes on the subscribe-then-backfill overlap window).
 *
 * Shared by [JVMPlugin]'s reactive `newObjectsFlow` subscription (point 6's "going forward" half) and
 * [backfillDefaultRoles] (point 6's one-time migration half), so the exact same rule governs both.
 *
 * @param rolesRepo Repo roles are granted through.
 * @param user User to grant default roles to.
 */
internal suspend fun grantDefaultRoles(rolesRepo: RolesRepo, user: RegisteredUser) {
    val subject = BaseRoleSubject.Direct(user.id.long.toString())
    rolesRepo.includeDirect(subject, UserRole)
    if (user.username.string == rootUsername) {
        rolesRepo.includeDirect(subject, SuperAdminRole)
    }
}

/**
 * One-time migration body (issue #68 point 6's "small migration"): applies [grantDefaultRoles] to
 * every currently-existing user. Extracted as a standalone, Koin/`VersionsRepo`-free function so it is
 * directly unit-testable — calling it twice in a row must be a no-op the second time (verifies
 * [grantDefaultRoles]'s `includeDirect` idempotency at the migration-body level, independent of
 * whatever gates how many times production actually invokes it, i.e. `VersionsRepo.setTableVersion`).
 *
 * @param usersRepo Source of all currently-existing users.
 * @param rolesRepo Repo roles are granted through.
 */
internal suspend fun backfillDefaultRoles(usersRepo: ReadUsersRepo, rolesRepo: RolesRepo) {
    usersRepo.getAll().values.forEach { user -> grantDefaultRoles(rolesRepo, user) }
}
```

**`src/jvmMain/kotlin/JVMPlugin.kt`** (**pass 1** ships the stock scaffold below the line; **pass 2**
replaces it with the real bootstrap wiring shown here — full "after" content, verified against
`/home/aleksey/projects/own/MicroUtils/repos/common/src/commonMain/kotlin/.../versions/{VersionsRepo,StandardVersionsRepo}.kt`
this step: `onUpdate: suspend T.(from: Int, to: Int) -> Unit` receives `Database` as its implicit receiver,
which the `{ _, _ -> backfillDefaultRoles(usersRepo, rolesRepo) }` lambda ignores and instead closes over
the two vals captured from `startPlugin`'s local scope — this compiles and behaves correctly since the
lambda body never needs to reference the `Database` receiver itself):

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.repos.versions.VersionsRepo
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM startup plugin for the `roles` server feature (issue #68 points 1–6).
 *
 * On [startPlugin]:
 * 1. **Subscribes first** to `UsersRepo.newObjectsFlow` — before reading any snapshot — so a user
 *    created concurrently by another plugin's `startPlugin` (e.g. `features/auth/server`'s root
 *    bootstrap; top-level plugins' `startPlugin`s run **concurrently**, not in `sample.config.json`
 *    list order — see `roles/README.md` Architecture Notes) is caught by this live subscription even
 *    if it races ahead of step 2's snapshot read. [grantDefaultRoles] is idempotent, so
 *    double-granting in the overlap window between steps 1 and 2 is harmless.
 * 2. Runs the one-time [backfillDefaultRoles] migration (issue point 6), gated by [VersionsRepo] so
 *    it executes exactly once across the app's lifetime, independent of restarts.
 *
 * Because the migration in step 2 runs against *all* currently-existing users and `root` (created by
 * `auth/server`'s bootstrap) counts as one of them, `root` naturally receives both the SuperAdmin role
 * (point 5) and the User role (point 6) with no special-casing.
 */
object JVMPlugin : StartPlugin {
    /** Version-tracking key under `tables_versions`; not a real schema table name. */
    private const val userRoleBackfillTableName = "users_default_role_backfill"

    /** Target version for [backfillDefaultRoles] — bump only if the backfill rule itself changes and must re-run. */
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

        usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user ->
            grantDefaultRoles(rolesRepo, user)
        }

        versionsRepo.setTableVersion(
            tableName = userRoleBackfillTableName,
            version = userRoleBackfillVersion,
            onUpdate = { _, _ ->
                backfillDefaultRoles(usersRepo, rolesRepo)
            }
        )
    }
}
```

Pass-1 stock version of this same file (what ships before pass 2 rewrites it — included for completeness
of the pass-1 file list):

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.roles.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.roles.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

**`src/commonMain/kotlin/utils/RequireRole.kt`** (new, **pass 3**):

```kotlin
package dev.inmo.wishlist.features.roles.server.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Pure allow/deny decision behind [requireRole], extracted from any Ktor coupling so it is directly
 * unit-testable (this repo has no precedent for constructing a fake [RoutingContext] — see
 * `roles/README.md` Architecture Notes). Denies (`false`) when [featureId] was never registered
 * (fail-closed on a typo) or when [callerId] lacks the registered role.
 *
 * @param featureId Symbolic id looked up in [FeatureRolesRegistry].
 * @param callerId Identity being checked.
 * @param rolesRepo Source of truth for role grants.
 * @return `true` when [featureId] is registered and [callerId] holds the required role.
 */
internal suspend fun isRoleRequirementSatisfied(
    featureId: String,
    callerId: UserId,
    rolesRepo: ReadRolesRepo
): Boolean {
    val requiredRole: BaseRole = FeatureRolesRegistry.requiredRole(featureId) ?: return false
    return rolesRepo.contains(BaseRoleSubject.Direct(callerId.long.toString()), requiredRole)
}

/**
 * Route-guard suspend function analogous to [getCallerUserIdOrAnswerUnauthorized]: resolves the
 * caller, then delegates the allow/deny decision to [isRoleRequirementSatisfied]. Responds `403
 * Forbidden` and returns `null` when denied; otherwise returns the caller's [UserId] so callers can
 * chain further logic, mirroring `AdminRoutingsConfigurator.requireAdmin()`'s existing `UserId?`
 * return shape.
 *
 * No production route calls this yet in issue #68's scope — point 8's three replacements call
 * `SimpleRolesFeature.isSuperAdmin` directly instead (see `roles/README.md` Architecture Notes for
 * why); this establishes a tested, ready-to-use guard for the next role-gated route.
 *
 * @param featureId Symbolic id looked up in [FeatureRolesRegistry].
 * @param rolesRepo Source of truth for role grants.
 * @return The caller's [UserId] when allowed; `null` after responding 401/403.
 */
suspend fun RoutingContext.requireRole(featureId: String, rolesRepo: ReadRolesRepo): UserId? {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
    if (!isRoleRequirementSatisfied(featureId, callerId, rolesRepo)) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return callerId
}
```

### 3.5 `features/roles/client` — deliberate permanent stub

All 5 files are the literal, unmodified scaffold output (`build.gradle`, `src/commonMain/kotlin/Plugin.kt`,
`src/jvmMain/kotlin/JVMPlugin.kt`, `src/jsMain/kotlin/JSPlugin.kt`, `src/androidMain/kotlin/AndroidPlugin.kt`)
with `{{$module_package}}` → `features.roles.client`. **Not** added as a dependency of `client/build.gradle`
and its platform plugins are **not** registered in any `Main.kt`/`MainActivity.kt` — nothing in issue #68
needs a client to talk to the general role graph, and kroles' own Ktor client/server module is deliberately
never used anywhere in this app (§3.1). This is a permanent state, not a to-do — flagged explicitly in
`roles/README.md` Architecture Notes (§5) so a future reader doesn't assume it's an oversight.

### 3.6 `features/simpleRoles/common` (KMP: JVM+JS+Android)

**`build.gradle`** — stock scaffold, unmodified:

```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
    id "com.android.library"
}

apply from: "$mppJvmJsAndroid"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.common.common")
            }
        }
    }
}
```

**`src/commonMain/kotlin/Constants.kt`** (new content):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.common

/**
 * Shared URL path segment constants for the `simpleRoles` feature.
 *
 * Consumed by both `simpleRoles/server`'s routing configurator and the client Ktor implementation to
 * keep path strings defined in a single place.
 */
object Constants {
    /** Root path segment for all `simpleRoles` routes: `/simpleRoles`. */
    const val prefixPathPart = "simpleRoles"

    /** Path segment for the superadmin-status probe: `/simpleRoles/isSuperAdmin`. */
    const val isSuperAdminPathPart = "isSuperAdmin"
}
```

**`src/commonMain/kotlin/Plugin.kt`**, **`src/jvmMain/kotlin/JVMPlugin.kt`**,
**`src/jsMain/kotlin/JSPlugin.kt`**, **`src/androidMain/kotlin/AndroidPlugin.kt`** — stock scaffold,
unmodified. No shared interface lives here: server and client `SimpleRolesFeature` signatures genuinely
differ (`UserId` arg vs. no-arg), which `agents/CODING.md`'s Full-Stack Feature pattern already
anticipates ("mirror the interface if it is not in common module of feature").

### 3.7 `features/simpleRoles/server` (JVM-only, `mppJavaProject`)

**`build.gradle`**:

```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
}

apply from: "$mppJavaProject"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.simpleRoles.common")
                api project(":wishlist.features.common.server")
                api project(":wishlist.features.roles.common")
                api project(":wishlist.features.auth.server")
                api project(":wishlist.features.users.common")
            }
        }
    }
}
```

**`src/commonMain/kotlin/SimpleRolesFeature.kt`** (new):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server

import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side capability exposing exactly one narrow check (issue #68 point 7): whether a given user
 * currently holds the SuperAdmin role. Deliberately does not expose any broader role-graph read/write
 * surface — see `roles/README.md` Architecture Notes for why the general
 * [dev.inmo.kroles.repos.RolesRepo] stays internal to `features/roles`.
 */
interface SimpleRolesFeature {
    /**
     * Returns whether [userId] currently holds the SuperAdmin role.
     *
     * @param userId Identity being checked.
     * @return `true` when [userId] holds SuperAdmin; `false` otherwise, including when [userId] is
     *   unknown to the roles repo.
     */
    suspend fun isSuperAdmin(userId: UserId): Boolean
}
```

**`src/commonMain/kotlin/services/SimpleRolesFeatureService.kt`** (new):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side [SimpleRolesFeature] implementation delegating to `features/roles`' [ReadRolesRepo].
 *
 * @param rolesRepo Read-only view of the role graph; only [ReadRolesRepo.contains] is used, so this
 *   class depends on the narrowest interface that can satisfy it.
 */
class SimpleRolesFeatureService(
    private val rolesRepo: ReadRolesRepo
) : SimpleRolesFeature {
    /**
     * Checks [userId] against the SuperAdmin role directly.
     *
     * @param userId Identity being checked.
     * @return `true` when [userId] holds SuperAdmin; `false` otherwise, including when [userId] is
     *   unknown to [rolesRepo].
     */
    override suspend fun isSuperAdmin(userId: UserId): Boolean =
        rolesRepo.contains(BaseRoleSubject.Direct(userId.long.toString()), SuperAdminRole)
}
```

**`src/commonMain/kotlin/configurators/SimpleRolesRoutingsConfigurator.kt`** (new):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.simpleRoles.common.Constants
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Ktor routing configurator for the `simpleRoles` feature.
 *
 * Registers a single bearer-authenticated endpoint under `/simpleRoles` (auto-prefixed to
 * `/api/simpleRoles` by the server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET /simpleRoles/isSuperAdmin` — resolves the caller from the bearer token and returns whether
 *   they hold SuperAdmin, via [feature]. `401 Unauthorized` on a missing/invalid token.
 *
 * @param feature Server-side [SimpleRolesFeature] implementation.
 */
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

**`src/commonMain/kotlin/Plugin.kt`** (customized):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server

import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.simpleRoles.server.configurators.SimpleRolesRoutingsConfigurator
import dev.inmo.wishlist.features.simpleRoles.server.services.SimpleRolesFeatureService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the `simpleRoles` server feature.
 *
 * Registers [SimpleRolesFeatureService] bound to [SimpleRolesFeature], and
 * [SimpleRolesRoutingsConfigurator] as an [ApplicationRoutingConfigurator.Element] with a random
 * qualifier so Ktor picks it up automatically.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { SimpleRolesFeatureService(get<ReadRolesRepo>()) }
        single<SimpleRolesFeature> { get<SimpleRolesFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            SimpleRolesRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
```

**`src/jvmMain/kotlin/JVMPlugin.kt`** — stock scaffold, unmodified:

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.simpleRoles.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.simpleRoles.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

### 3.8 `features/simpleRoles/client` (KMP+Compose: JVM+JS+Android)

**`build.gradle`**:

```groovy
plugins {
    id "org.jetbrains.kotlin.multiplatform"
    id "org.jetbrains.kotlin.plugin.serialization"
    id "com.android.library"
    alias(libs.plugins.compose)
    alias(libs.plugins.kt.compose)
}

apply from: "$mppJvmJsAndroidWithCompose"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api project(":wishlist.features.simpleRoles.common")
                api project(":wishlist.features.common.client")
                api project(":wishlist.features.auth.client")
                api project(":wishlist.features.users.common")
            }
        }
    }
}
```

**`src/commonMain/kotlin/SimpleRolesFeature.kt`** (new):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.client

/**
 * Client-side capability exposing exactly one narrow check (issue #68 point 7): whether the currently
 * authenticated caller holds the SuperAdmin role. No `UserId` parameter — the server resolves the
 * caller from the bearer token, mirroring every other authenticated client call in this app.
 */
interface SimpleRolesFeature {
    /**
     * Returns whether the currently authenticated caller holds the SuperAdmin role.
     *
     * @return `true` when authenticated as SuperAdmin; `false` otherwise, including when anonymous or
     *   on any request failure (both realizations fail closed — see [KtorSimpleRolesFeature] and
     *   [CacheSimpleRolesFeature]).
     */
    suspend fun isSuperAdmin(): Boolean
}
```

**`src/commonMain/kotlin/KtorSimpleRolesFeature.kt`** (new — realization 1 of 2, mirrors
`KtorAuthFeature.isRegistrationAvailable()`'s exact fail-closed shape, read directly this step):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.simpleRoles.common.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

/**
 * HTTP-only [SimpleRolesFeature] realization (issue #68 point 10, realization 1 of 2). Performs no
 * caching or business logic, per the "Ktor realization rule" (`agents/CODING.md`).
 *
 * @param client Shared app-wide [HttpClient].
 */
class KtorSimpleRolesFeature(private val client: HttpClient) : SimpleRolesFeature {
    private val isSuperAdminPath = "${Constants.prefixPathPart}/${Constants.isSuperAdminPathPart}"

    /**
     * Calls `GET /simpleRoles/isSuperAdmin`.
     *
     * @return The decoded response body; `false` on any non-success status or request failure
     *   (network error, missing/expired bearer token before refresh, etc.).
     */
    override suspend fun isSuperAdmin(): Boolean = runCatchingLogging {
        val response = client.get(isSuperAdminPath)
        if (!response.status.isSuccess()) return@runCatchingLogging false
        response.body<Boolean>()
    }.getOrDefault(false)
}
```

**`src/commonMain/kotlin/CacheSimpleRolesFeature.kt`** (new — realization 2 of 2, keyed off `meStateFlow`
per `002-planning.md` §3.3):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * Caching [SimpleRolesFeature] realization (issue #68 point 10, realization 2 of 2). Caches only the
 * boolean answer, keyed off [meState] rather than a bare login/logout flag — this also re-fetches
 * when the authenticated *identity* changes within one session (e.g. logout then a different login),
 * not only on a true/false auth-state flip. Mirrors `features/auth/client/Plugin.kt`'s `meStateFlow`
 * refresh pattern exactly.
 *
 * Exposes [isSuperAdminStateFlow] beyond the [SimpleRolesFeature] interface, following the "Typed
 * definition & accessor helpers" pattern (`agents/CODING.md`) rather than widening the deliberately
 * narrow point-7 interface — see `simpleRoles/README.md` Architecture Notes.
 *
 * @param delegate HTTP realization this class refreshes its cache from.
 * @param meState Authenticated-caller state; both a login/logout signal and an identity-change signal.
 * @param scope Coroutine scope the refresh subscription runs on.
 */
class CacheSimpleRolesFeature(
    private val delegate: KtorSimpleRolesFeature,
    private val meState: StateFlow<RegisteredUser?>,
    scope: CoroutineScope
) : SimpleRolesFeature {
    private val cached = MutableStateFlow(false)

    /** Read-only view of the cached answer, for consumers that need reactive access (e.g. `ui/users`). */
    val isSuperAdminStateFlow: StateFlow<Boolean> get() = cached.asStateFlow()

    init {
        merge(flowOf(Unit), meState).subscribeLoggingDropExceptions(scope) {
            cached.value = runCatchingLogging { delegate.isSuperAdmin() }.getOrDefault(false)
        }
    }

    /**
     * Returns the last cached answer. Never itself performs a network call — always up to date
     * within one [meState] emission's latency (login, logout, or identity change).
     */
    override suspend fun isSuperAdmin(): Boolean = cached.value
}
```

**`src/commonMain/kotlin/Plugin.kt`** (customized):

```kotlin
package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.meStateFlow
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common startup plugin for the `simpleRoles` client feature.
 *
 * Registers [KtorSimpleRolesFeature] (HTTP-only) and [CacheSimpleRolesFeature] (the bound
 * [SimpleRolesFeature] implementation — caches the boolean answer, refreshed off
 * `features/auth/client`'s `meStateFlow`).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorSimpleRolesFeature(get()) }
        single { CacheSimpleRolesFeature(get(), meStateFlow, get()) }
        single<SimpleRolesFeature> { get<CacheSimpleRolesFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
```

**`src/jvmMain/kotlin/JVMPlugin.kt`**, **`src/jsMain/kotlin/JSPlugin.kt`**,
**`src/androidMain/kotlin/AndroidPlugin.kt`** — stock scaffold, unmodified (substitute
`{{$module_package}}` → `features.simpleRoles.client`).

---

## 4. Coding-pass split — refined to **4 passes** (was proposed as 5)

`002-planning.md` §9 proposed 5 passes. I confirm the overall shape (foundation → bootstrap → aggregator →
call-site replacement, security-sensitive pass kept smallest/last-before-docs) but **refine it to 4
passes**, for two concrete reasons found while making the plan buildable — both are wiring/sequencing
corrections, not design changes:

1. **README pass folded into passes 1 and 4, not a standalone pass 5.** Round 2 deferred all 8 README
   edits to a final pass "so they describe the actually-landed code rather than the plan." That reasoning
   made sense when the *plan* was still evolving. It no longer applies: this document (Architecture) has
   already fully specified the end-to-end design for the whole feature, across every pass, in byte-exact
   form. There is nothing left to learn from Coding passes 2–3 that would change what `roles/README.md`
   and `simpleRoles/README.md` should say — so Coding pass 1 writes their **complete, final** content
   immediately (§5 below) when the directories are first created, which also satisfies `agents/ALL.md`'s
   "every feature directory MUST contain a README.md" rule from the first moment the directories exist,
   rather than leaving two feature directories README-less for 3 passes. The 6 **existing** touched
   features' README updates move into pass 4, alongside the code changes that make them stale — this is
   the literal reading of `agents/CODING.md`'s "after any code change... update the feature README" rule
   (updating them in a same-numbered-but-separate pass would mean pass 4 lands with stale docs for one
   Coding invocation).
2. **`roles.server.JVMPlugin` must be registered in both server config files starting in pass 1, not
   deferred to pass 2.** `simpleRoles/server`'s `SimpleRolesFeatureService` resolves `ReadRolesRepo` from
   Koin lazily (inside its `single { }` definition block). That binding only exists in the DI graph if
   `roles.common.JVMPlugin.setupDI` has run — and the only way that happens, per `agents/CODING.md`'s rule
   that a plugin may only call `setupDI`/`startPlugin` of a plugin in its *own* feature, is if
   `roles.server.JVMPlugin` (which does `with(roles.common.JVMPlugin) { setupDI(config) }`) is itself in
   the config's `"plugins"` list. `simpleRoles/server/JVMPlugin` cannot reach into `roles.common.JVMPlugin`
   directly — that would violate the same rule (different feature). So for pass 1's own verification step
   ("simpleRoles's route independently curl-able returning `false`") to actually work rather than 500 on a
   missing Koin binding, `roles.server.JVMPlugin` must already be config-registered in pass 1. This is
   harmless at that point — pass 1's `roles/server/JVMPlugin.kt` is still the stock, side-effect-free
   scaffold (§3.4), so registering it early only makes the (empty) `RolesRepo` binding available; it does
   not run any bootstrap logic before pass 2 adds it.

Both corrections are additive/sequencing fixes to round 2's proposal, in the same spirit as round 2's own
§0 correction to round 1 — found by tracing exact Koin/DI mechanics, not by second-guessing the design.

### Pass 1 — Foundation (scaffolding + `roles/common` DI layer + full `simpleRoles` + both new READMEs)

**Delivers:** Gradle version-catalog changes; `settings.gradle`; both features scaffolded (all 12
submodule directories); `roles/common`'s model/repo/DI layer populated (§3.3, **except**
`FeatureRolesRegistry.kt`/`RoleGatedFeatureIds` and `Plugin.kt`'s population — those are pass 3, so pass
1's `roles/common/Plugin.kt` stays the stock empty scaffold); `roles/server` and `roles/client` scaffolded
in stock form (§3.4's stock `JVMPlugin.kt`, §3.5); `simpleRoles` fully implemented server+client (§3.6–3.8);
both new READMEs written complete (§5); both server config files gain `roles.server.JVMPlugin` (stock,
harmless) and `simpleRoles.server.JVMPlugin`; `simpleRoles.client` wired into the top-level client
(`client/build.gradle` + all 3 platform entry points) so it's resolvable end-to-end even though nothing
consumes it yet (pass 4 is the first consumer) — **not** wired in pass 1 in an earlier draft of this split;
moved here because it costs nothing to include with the rest of "feature genuinely complete" and avoids a
5th otherwise-tiny wiring-only touch to `client/*` later. *(Re-check at Coding time: if this is judged to
add unwanted churn to files pass 4 also needs to touch, it is safe to move these 4 files into pass 4
instead — flagged as the one truly interchangeable item in this split.)*

**File list:**
- `gradle/libs.versions.toml`
- `settings.gradle`
- `features/roles/common/build.gradle`
- `features/roles/common/src/commonMain/kotlin/Constants.kt`
- `features/roles/common/src/commonMain/kotlin/RoleConstants.kt`
- `features/roles/common/src/commonMain/kotlin/Plugin.kt` (stock)
- `features/roles/common/src/jvmMain/kotlin/repo/RolesRepoFactory.kt`
- `features/roles/common/src/jvmMain/kotlin/JVMPlugin.kt`
- `features/roles/common/src/jsMain/kotlin/JSPlugin.kt` (stock)
- `features/roles/common/src/androidMain/kotlin/AndroidPlugin.kt` (stock)
- `features/roles/server/build.gradle` (scaffold default)
- `features/roles/server/src/commonMain/kotlin/Plugin.kt` (stock)
- `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt` (stock)
- `features/roles/client/build.gradle`, `src/commonMain/kotlin/Plugin.kt`,
  `src/jvmMain/kotlin/JVMPlugin.kt`, `src/jsMain/kotlin/JSPlugin.kt`,
  `src/androidMain/kotlin/AndroidPlugin.kt` (all stock)
- `features/simpleRoles/common/build.gradle` (stock)
- `features/simpleRoles/common/src/commonMain/kotlin/Constants.kt`
- `features/simpleRoles/common/src/commonMain/kotlin/Plugin.kt` (stock)
- `features/simpleRoles/common/src/{jvmMain,jsMain,androidMain}/kotlin/{JVMPlugin,JSPlugin,AndroidPlugin}.kt` (stock)
- `features/simpleRoles/server/build.gradle`
- `features/simpleRoles/server/src/commonMain/kotlin/SimpleRolesFeature.kt`
- `features/simpleRoles/server/src/commonMain/kotlin/services/SimpleRolesFeatureService.kt`
- `features/simpleRoles/server/src/commonMain/kotlin/configurators/SimpleRolesRoutingsConfigurator.kt`
- `features/simpleRoles/server/src/commonMain/kotlin/Plugin.kt`
- `features/simpleRoles/server/src/jvmMain/kotlin/JVMPlugin.kt` (stock)
- `features/simpleRoles/server/src/commonTest/kotlin/services/FakeRolesRepo.kt`
- `features/simpleRoles/server/src/commonTest/kotlin/services/SimpleRolesFeatureServiceTest.kt`
- `features/simpleRoles/client/build.gradle`
- `features/simpleRoles/client/src/commonMain/kotlin/SimpleRolesFeature.kt`
- `features/simpleRoles/client/src/commonMain/kotlin/KtorSimpleRolesFeature.kt`
- `features/simpleRoles/client/src/commonMain/kotlin/CacheSimpleRolesFeature.kt`
- `features/simpleRoles/client/src/commonMain/kotlin/Plugin.kt`
- `features/simpleRoles/client/src/{jvmMain,jsMain,androidMain}/kotlin/{JVMPlugin,JSPlugin,AndroidPlugin}.kt` (stock)
- `client/build.gradle`
- `client/src/jsMain/kotlin/Main.kt`
- `client/src/jvmMain/kotlin/Main.kt`
- `client/android/src/main/kotlin/MainActivity.kt`
- `server/sample.config.json`
- `server/dev.config.json`
- `features/roles/README.md` (new, complete)
- `features/simpleRoles/README.md` (new, complete)

**Verify:** full project build green; `simpleRoles/server`'s and `roles/common`'s unit tests green (no
`roles/server`/`roles/common` tests exist yet at this point — those land in passes 2/3); manual boot +
`GET /api/simpleRoles/isSuperAdmin` with a valid bearer token returns `false` for any real user (the
`roles` table exists and is empty — nobody has been granted anything yet, since pass 2 hasn't landed).

### Pass 2 — Bootstrap + migration

**Delivers:** `roles/server`'s real bootstrap/migration logic (§3.4's "after" `JVMPlugin.kt` + new
`RolesBootstrap.kt`), the two extra `roles/server/build.gradle` dependencies, and its test suite.

**File list:**
- `features/roles/server/build.gradle` (add `users.common`, `auth.server`)
- `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt`
- `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt` (rewrite — real logic)
- `features/roles/server/src/commonTest/kotlin/FakeRolesRepo.kt`
- `features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt`
- `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt`

**Verify:** full build green; `RolesBootstrapTest` green; manual: fresh-DB boot grants `root` both
SuperAdmin and User (confirm via a temporary log line or a direct `roles` table query — no admin UI exists
yet to check this through, since that lands in pass 4); re-running against an existing DB does not
duplicate rows / does not re-run the migration a second time (`tables_versions` row for
`users_default_role_backfill` stays at version 1).

### Pass 3 — Aggregator + guard

**Delivers:** `FeatureRolesRegistry`/`RoleGatedFeatureIds` (§3.3), populates `roles/common/Plugin.kt` (was
stock since pass 1), `RequireRole.kt` (§3.4), and both test files. Purely additive — no existing call site
is touched yet.

**File list:**
- `features/roles/common/src/commonMain/kotlin/FeatureRolesRegistry.kt`
- `features/roles/common/src/commonMain/kotlin/Plugin.kt` (populate — was stock)
- `features/roles/common/src/commonTest/kotlin/FeatureRolesRegistryTest.kt`
- `features/roles/server/src/commonMain/kotlin/utils/RequireRole.kt`
- `features/roles/server/src/commonTest/kotlin/utils/RequireRoleTest.kt`

**Verify:** full build green; `FeatureRolesRegistryTest` + `RequireRoleTest` green.

### Pass 4 — Call-site replacement (security-sensitive) + existing README updates

**Delivers:** the 3 server + 1 client privilege-check rewrites (§6), the 3 consumer `build.gradle`
additions (`admin/server`, `email/server`, `files/server` → `simpleRoles.server`; `ui/users` →
`simpleRoles.client`), the `EmailFeatureServiceTest` re-point + new `FakeSimpleRolesFeature`, and the 6
existing-feature README updates (§7) landed in the same pass as the code that makes them stale, per
`agents/CODING.md`'s rule. Deliberately kept as the smallest, most reviewable pass — the one where a
mistake has the highest blast radius (privilege-check replacement).

*(If pass 1's `client/*` wiring is instead deferred per the note in pass 1, those same 4 files move here.)*

**File list:**
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt`
- `features/admin/server/src/commonMain/kotlin/Plugin.kt`
- `features/admin/server/build.gradle`
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`
- `features/email/server/src/commonMain/kotlin/Plugin.kt`
- `features/email/server/build.gradle`
- `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`
- `features/email/server/src/commonTest/kotlin/services/FakeSimpleRolesFeature.kt`
- `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt`
- `features/files/server/src/commonMain/kotlin/Plugin.kt`
- `features/files/server/build.gradle`
- `features/ui/users/src/commonMain/kotlin/Plugin.kt`
- `features/ui/users/build.gradle`
- `features/admin/README.md`
- `features/email/README.md`
- `features/files/README.md`
- `features/ui/users/README.md`
- `features/users/README.md`
- `features/auth/README.md`

**Verify:** full build green; `EmailFeatureServiceTest` green; manual: a non-root/non-`SuperAdmin` bearer
token gets `403` from `/admin/...` and from `PUT /files/avatar/{otherUserId}`, and `false` from
`POST /email/sendTest`; the `root` bearer token (now holding `SuperAdmin` via pass 2's bootstrap) gets the
previous, unchanged behavior for all four; client-side, a non-superadmin logged-in user still cannot see
the root-only edit affordances in `features/ui/users`.

### Ordering dependencies (unchanged from round 2, restated for the refined split)

Pass 2 depends only on pass 1 (needs `RolesRepo`/`UsersRepo`/`VersionsRepo` resolvable). Pass 3 depends
only on pass 1 (needs `roles/common` to exist) and has no dependency on pass 2 — **could be run before
pass 2** if preferred; kept as pass 3 here only because grouping "storage layer" (pass 2) before
"cross-cutting policy infrastructure" (pass 3) reads more naturally. Pass 4 depends on pass 1
(`SimpleRolesFeature` must exist and be wired) but not on pass 2 or 3 — the replacement's *correctness*
doesn't depend on the bootstrap having run (pass 2 is what makes the *answer* `true` for `root` in
practice). Recommend running strictly in the 1→2→3→4 order shown regardless, since pass 4 being last and
smallest is the property worth preserving for review purposes.

---

## 5. New README content (task item 6) — full files

Per `agents/ALL.md`'s Feature README Rule, both new feature directories get a `README.md` with an empty
`## Operator Notes` section (nothing to write there — no operator constraint beyond what
`features/admin/README.md`'s existing Operator Notes already say, which stays untouched) plus full
Overview/Routes/Models/Architecture Notes, written now per §4's reasoning for folding README work into
pass 1.

### `features/roles/README.md`

```markdown
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
| `FeatureRolesRegistry` | `roles/common` | In-memory `featureId -> BaseRole` map; `register`/`requiredRole`. Populated by `roles/common/Plugin.setupDI`. |
| `RoleGatedFeatureIds` | `roles/common` | `const val` symbolic ids registered against `FeatureRolesRegistry` (`adminPanel`, `filesAvatarChangeForOthers`, `emailSendTest`). |
| `requireRole` / `isRoleRequirementSatisfied` | `roles/server/utils` | Route-guard helper (`RoutingContext.requireRole`) and its pure allow/deny decision function. |
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
  general `roles` `RolesRepo`. `requireRole` is a real, tested, ready-to-use guard for the next
  role-gated route — this gap between "registry has data" and "guard has a caller" is intentional,
  not an oversight.
- **Registration is centralized, not self-registered.** `FeatureRolesRegistry.register(...)` calls for
  all three of today's role-gated capabilities live in `roles/common/Plugin.setupDI`, not in each of
  `admin/server`/`files/server`/`email/server`'s own `Plugin.setupDI`. This avoids giving those three
  otherwise-unrelated modules a new Gradle dependency on `roles/common` purely for a registration
  side-effect, when point 8 already gives them a dependency on `simpleRoles/server` instead — keeping
  the security-sensitive call-site-replacement change minimal. A future feature that actually calls
  `requireRole` for its own gating should self-register from its own `Plugin.setupDI` at that point.
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
```

### `features/simpleRoles/README.md`

```markdown
# Feature: SimpleRoles

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Narrow, full-stack "am I superadmin" feature (issue #68 point 7). Exposes exactly one capability —
whether a user currently holds the SuperAdmin role — deliberately without any broader access to the
general role graph owned by `features/roles`. This is the feature every root-privilege check in the
app (issue #68 points 8/9) now goes through.

## Routes

> Served under the global `/api` prefix (e.g. `/api/simpleRoles/isSuperAdmin`), applied centrally by
> `features/common/server`.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/simpleRoles/isSuperAdmin` | Bearer | — → `Boolean` | Returns whether the authenticated caller (resolved from the bearer token) currently holds SuperAdmin |

## Models

| Type | Module | Description |
|------|--------|-------------|
| `SimpleRolesFeature` (server) | `simpleRoles/server` | `suspend fun isSuperAdmin(userId: UserId): Boolean` — caller identity passed explicitly, matching this app's server-feature convention. Implemented by `SimpleRolesFeatureService`. |
| `SimpleRolesFeature` (client) | `simpleRoles/client` | `suspend fun isSuperAdmin(): Boolean` — no argument; the server resolves the caller from the bearer token. **Not** mirrored 1:1 with the server interface — the signatures genuinely differ (arg vs. no-arg), which `agents/CODING.md`'s Full-Stack Feature pattern already anticipates. |
| `SimpleRolesFeatureService` | `simpleRoles/server` | Delegates to `features/roles`' `ReadRolesRepo.contains(...)`. |
| `KtorSimpleRolesFeature` | `simpleRoles/client` | HTTP-only realization (point 10, 1 of 2); no caching or logic beyond the call. |
| `CacheSimpleRolesFeature` | `simpleRoles/client` | Caching realization (point 10, 2 of 2); caches only the boolean answer, refreshed off `features/auth/client`'s `meStateFlow`. Also exposes `isSuperAdminStateFlow: StateFlow<Boolean>` beyond the `SimpleRolesFeature` interface — see Architecture Notes. Bound as the `SimpleRolesFeature` implementation in Koin. |

## Architecture Notes

- **Ktor + Cache split (point 10):** `KtorSimpleRolesFeature` performs the HTTP call only, per this
  codebase's "Ktor realization rule" (`agents/CODING.md`). `CacheSimpleRolesFeature` wraps it and is
  the concrete type bound as `SimpleRolesFeature` in `Plugin.setupDI` — every consumer that injects
  the interface gets the cached path.
- **Cache invalidation is keyed off `meStateFlow`, not a bare login/logout boolean.** Mirrors
  `features/auth/client/Plugin.kt`'s own `meStateFlow` refresh pattern
  (`merge(flowOf(Unit), meState).subscribeLoggingDropExceptions(scope) { ... }`) exactly. Using the
  richer `StateFlow<RegisteredUser?>` (rather than `AuthCredentialsStorage.userAuthorised: StateFlow<Boolean>`
  directly) means the cache also correctly re-fetches when the authenticated *identity* changes within
  one session (e.g. logout then a different login), not only on a bare auth-state flip.
- **`isSuperAdminStateFlow` is a deliberate, narrow exception to "depend on interfaces."**
  `UsersModel.isCurrentUserRootFlow` (see `features/ui/users/README.md`) needs a reactive
  `StateFlow<Boolean>` for its existing `combine(...)`/`.stateIn(...)` wiring, but point 7's interface
  is deliberately narrow (`suspend fun isSuperAdmin(): Boolean`, no `StateFlow`) and widening it would
  violate that scope. `CacheSimpleRolesFeature` exposes `isSuperAdminStateFlow` as an additional,
  concrete-type-only property — `features/ui/users/Plugin.kt` injects `CacheSimpleRolesFeature`
  directly (not the `SimpleRolesFeature` interface) specifically to read it, following the same "Typed
  definition & accessor helpers" shape this codebase already uses for `Koin.meStateFlow`/
  `Koin.secretMeMutableStateFlow`.
- **Dependencies:** `simpleRoles/server` depends on `roles/common` (for `ReadRolesRepo`/`SuperAdminRole`),
  `features/common/server`, `features/auth/server` (bearer caller resolution), `features/users/common`
  (`UserId`). `simpleRoles/client` depends on `features/common/client`, `features/auth/client` (for
  `meStateFlow`), `features/users/common` (for `RegisteredUser` in `meState`'s type).
```

---

## 6. The 4 existing root-check call sites — exact before/after diffs (task item 2)

Re-audited independently this step via `ast-index usages`/`ast-index search` (§1) — confirms round 2's
audit exhaustively: exactly these 4 sites, no others.

### 6.1 Server — `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt`

**Before** (current file, lines 60–78 — full file read this step):

```kotlin
class AdminRoutingsConfigurator(
    private val adminFeature: AdminFeature,
    private val usersRepo: ReadUsersRepo,
    private val wishlistService: WishlistService,
    private val wishlistRepo: WishlistRepo,
    private val wishlistItemRepo: WishlistItemRepo
) : ApplicationRoutingConfigurator.Element {

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

**After:**

```kotlin
class AdminRoutingsConfigurator(
    private val adminFeature: AdminFeature,
    private val usersRepo: ReadUsersRepo,
    private val wishlistService: WishlistService,
    private val wishlistRepo: WishlistRepo,
    private val wishlistItemRepo: WishlistItemRepo,
    private val simpleRolesFeature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {

    private suspend fun RoutingContext.requireAdmin(): UserId? {
        val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
        if (!simpleRolesFeature.isSuperAdmin(callerId)) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }
        return callerId
    }
```

`usersRepo: ReadUsersRepo` is **kept** — still used by `GET /admin/users/getById/{id}` elsewhere in the
same class (verified by re-reading the full file this step: `usersRepo.getById(id)` at the
`usersGetByIdPathPart` route). Add one import:

```diff
+import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
```

Update the class-level KDoc's "the authenticated caller must be the `root` user" line (lines 34–38) to
say "must hold the SuperAdmin role" — mechanical wording change, not shown verbatim here to keep this
section focused on the functional diff; apply per the KDoc-accuracy rule in `agents/CODING.md`.

**DI wiring** — `features/admin/server/src/commonMain/kotlin/Plugin.kt`:

```diff
         singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
             AdminRoutingsConfigurator(
                 adminFeature = get(),
                 usersRepo = get<UsersRepo>(),
                 wishlistService = get<WishlistService>(),
                 wishlistRepo = get<WishlistRepo>(),
-                wishlistItemRepo = get<WishlistItemRepo>()
+                wishlistItemRepo = get<WishlistItemRepo>(),
+                simpleRolesFeature = get<SimpleRolesFeature>()
             )
         }
```

Add import `dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature`.

**Gradle** — `features/admin/server/build.gradle`:

```diff
     sourceSets {
         commonMain {
             dependencies {
                 api project(":wishlist.features.admin.common")
                 api project(":wishlist.features.auth.server")
                 api project(":wishlist.features.wishlist.server")
+                api project(":wishlist.features.simpleRoles.server")
             }
         }
     }
```

### 6.2 Server — `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`

**Before** (current working-tree file, already once-modified per this branch's `git status`, read fully
this step):

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] is always a real, non-null [EmailsService] — [dev.inmo.wishlist.features.email.server.Plugin]
 * only ever constructs this class when one is registered (SMTP configured). When no [EmailsService] is
 * registered (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature]
 * binding instead. Because of that, [isFeatureEnabled] always returns `true`.
 *
 * Resolves the caller's user record from [usersRepo] to enforce access rules and to perform
 * email-address storage updates:
 * - [sendTestEmail] verifies [callerId] is root before delegating SMTP delivery via [emailsService].
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService].
 *
 * @param emailsService SMTP delivery service used for sends. Always non-null — see class doc.
 * @param usersRepo User repository used for privilege checking and email-address persistence.
 */
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val usersRepo: UsersRepo
) : EmailFeature {

    /** Username [sendTestEmail] compares the caller's username against to gate test-email sends to the root account. */
    private val rootUsername = "root"

    override suspend fun isFeatureEnabled(): Boolean = true

    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        val caller = usersRepo.getById(callerId) ?: return false
        if (caller.username.string != rootUsername) return false
        return emailsService.sendText(
            recipient = recipient,
            subject = "Test email from WishlistApp",
            text = "This is a test email sent from WishlistApp to verify SMTP configuration."
        )
    }

    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
```

**After** (full file):

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] is always a real, non-null [EmailsService] — [dev.inmo.wishlist.features.email.server.Plugin]
 * only ever constructs this class when one is registered (SMTP configured). When no [EmailsService] is
 * registered (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature]
 * binding instead. Because of that, [isFeatureEnabled] always returns `true`.
 *
 * - [sendTestEmail] verifies [callerId] is superadmin (via [simpleRolesFeature]) before delegating SMTP
 *   delivery via [emailsService].
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService] and of superadmin status.
 *
 * @param emailsService SMTP delivery service used for sends. Always non-null — see class doc.
 * @param usersRepo User repository used for email-address persistence.
 * @param simpleRolesFeature Superadmin-status check used to gate [sendTestEmail]; see
 *   `features/roles`/`features/simpleRoles` (issue #68).
 */
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val usersRepo: UsersRepo,
    private val simpleRolesFeature: SimpleRolesFeature
) : EmailFeature {

    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return Always `true` — this class is only ever constructed with a real [emailsService]; see
     *   [DisabledEmailFeature] for the SMTP-disabled no-op path.
     */
    override suspend fun isFeatureEnabled(): Boolean = true

    /**
     * Sends a test email to [recipient] if [callerId] is superadmin.
     *
     * @param callerId Caller checked against [simpleRolesFeature].
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege or SMTP
     *   delivery fails.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        if (!simpleRolesFeature.isSuperAdmin(callerId)) return false
        return emailsService.sendText(
            recipient = recipient,
            subject = "Test email from WishlistApp",
            text = "This is a test email sent from WishlistApp to verify SMTP configuration."
        )
    }

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical to [DisabledEmailFeature.setMyEmail].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [email] is already stored for a different user; propagates unchanged from
     *   [updateStoredEmail] / `UsersRepo.update` — this method does not catch it.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
```

Note: the previous "caller not found in `usersRepo` → `false`" branch collapses naturally —
`simpleRolesFeature.isSuperAdmin` on an unknown `UserId` resolves `contains(...)` over an empty role set,
i.e. `false` — same net behavior, one fewer `usersRepo.getById` round trip.

**DI wiring** — `features/email/server/src/commonMain/kotlin/Plugin.kt`:

```diff
         single<EmailFeature> {
             getOrNull<EmailsService>() ?.let {
-                EmailFeatureService(it, get<UsersRepo>())
+                EmailFeatureService(it, get<UsersRepo>(), get<SimpleRolesFeature>())
             } ?: DisabledEmailFeature(get<UsersRepo>())
         }
```

Add import `dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature`.

**Gradle** — `features/email/server/build.gradle`:

```diff
     sourceSets {
         commonMain {
             dependencies {
                 api project(":wishlist.features.email.common")
                 api project(":wishlist.features.common.server")
                 api project(":wishlist.features.users.common")
                 api project(":wishlist.features.auth.server")
+                api project(":wishlist.features.simpleRoles.server")
                 api libs.angus.mail
             }
         }
     }
```

### 6.3 Server — `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt`

**Before** (full file relevant excerpts, read this step):

```kotlin
class FilesRoutingsConfigurator(
    private val filesService: FilesService,
    private val usersRepo: ReadUsersRepo
) : ApplicationRoutingConfigurator.Element {
    private val rootUsername = "root"

    /** `true` when [callerId] resolves to the `root` user. */
    private suspend fun isRoot(callerId: UserId): Boolean =
        usersRepo.getById(callerId)?.username?.string == rootUsername

    override fun Route.invoke() {
        route(Constants.filesPrefixPathPart) {
            /* ... unauthenticated routes unchanged ... */
            authenticate {
                post(Constants.finalizePathPart) { /* unchanged */ }
                put("${Constants.avatarPathPart}/{userId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    if (callerId != userId && !isRoot(callerId)) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@put
                    }
                    val fileId = call.receive<FileId>()
                    if (filesService.setAvatar(userId, fileId)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
```

**After** (relevant excerpts; unauthenticated routes and the `finalize` route body are byte-identical, not
repeated):

```kotlin
class FilesRoutingsConfigurator(
    private val filesService: FilesService,
    private val simpleRolesFeature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.filesPrefixPathPart) {
            /* ... unauthenticated routes unchanged ... */
            authenticate {
                post(Constants.finalizePathPart) { /* unchanged */ }
                put("${Constants.avatarPathPart}/{userId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    if (callerId != userId && !simpleRolesFeature.isSuperAdmin(callerId)) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@put
                    }
                    val fileId = call.receive<FileId>()
                    if (filesService.setAvatar(userId, fileId)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
```

`usersRepo: ReadUsersRepo` is **removed entirely** — re-read the full file this step and confirmed `isRoot`
was its only use. Imports change:

```diff
-import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
+import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
```

`UserId` import stays — still used by `call.parameters["userId"]?.toLongOrNull()?.let(::UserId)` in two
routes. Update the class-level KDoc's `@param usersRepo` line (currently: "Used to resolve whether the
authenticated caller is the `root` user when authorizing avatar changes for another user.") to a
`@param simpleRolesFeature` line describing the same role, per the KDoc-accuracy rule.

**DI wiring** — `features/files/server/src/commonMain/kotlin/Plugin.kt`:

```diff
         single { FilesService(get(), get(), get(), get()) }
         singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
-            FilesRoutingsConfigurator(get(), get())
+            FilesRoutingsConfigurator(get(), get<SimpleRolesFeature>())
         }
```

Add import `dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature`.

**Gradle** — `features/files/server/build.gradle`:

```diff
     sourceSets {
         commonMain {
             dependencies {
                 api project(":wishlist.features.files.common")
                 api project(":wishlist.features.common.server")
                 api project(":wishlist.features.auth.server")
+                api project(":wishlist.features.simpleRoles.server")
                 api libs.microutils.ktor.server
             }
         }
     }
```

### 6.4 Client — `features/ui/users/src/commonMain/kotlin/Plugin.kt`

**Before** (lines 76–78, full file read this step):

```kotlin
                override val isCurrentUserRootFlow: StateFlow<Boolean> =
                    meState.map { it?.username?.string == "root" }
                        .stateIn(scope, SharingStarted.Eagerly, meState.value?.username?.string == "root")
```

**After:**

```kotlin
                override val isCurrentUserRootFlow: StateFlow<Boolean> =
                    get<CacheSimpleRolesFeature>().isSuperAdminStateFlow
```

Per §2.2's decision, the property name (`isCurrentUserRootFlow`) and its type (`StateFlow<Boolean>`) are
unchanged — only the source of the flow changes. `scope`/`stateIn`/`SharingStarted` may become unused in
this file if nothing else in `Plugin.kt`'s `single<UsersModel> { ... }` block still needs them — re-check
at Coding time (`currentUserIdFlow`, defined two lines above, still uses `meState.map { it?.id }.stateIn(...)`,
so `scope`/`stateIn`/`SharingStarted` stay in use; no import becomes dead).

Add import:

```diff
+import dev.inmo.wishlist.features.simpleRoles.client.CacheSimpleRolesFeature
```

**Gradle** — `features/ui/users/build.gradle`:

```diff
     sourceSets {
         commonMain {
             dependencies {
                 api project(":wishlist.features.common.client")
                 api project(":wishlist.features.ui.topBar")
                 api project(":wishlist.features.users.client")
                 api project(":wishlist.features.auth.client")
                 api project(":wishlist.features.admin.client")
                 api project(":wishlist.features.files.client")
+                api project(":wishlist.features.simpleRoles.client")
             }
         }
     }
```

**Downstream consumers need no code change** — `UserViewModel.canEditState`, `UserEditViewModel.isRootState`/
`canSaveState`, and the three platform `UserEditView.kt`s all read `UsersModel.isCurrentUserRootFlow`
(or, for `UserEditViewModel`, re-expose it as `isRootState`) unchanged; only the *mechanism* behind that
flow changed. **Guidance only, not a mandatory byte-exact diff** (per §2.2, this is prose/KDoc-only): the
KDoc on `UsersModel.isCurrentUserRootFlow` ("the only identity permitted to edit... derived from... the
`root` user") and the doc comments on `UserViewModel.canEditState`/`UserEditViewModel.isRootState`/
`canSaveState` that say "the caller is `root`" read as literally true today (SuperAdmin is fixed to `root`
forever, per Q1) but describe the *old* mechanism; Coding may update this prose to "SuperAdmin" wording
while landing pass 4, or leave it for a follow-up — it does not block correctness.

---

## 7. Existing-README updates (task item 6, continued) — precise bullet-level diffs

Applied in Coding pass 4, alongside the code change each documents. Given as precise before/after text
(not full-file dumps) since these are large, already-good files needing small Architecture Notes edits.

### `features/admin/README.md`

Replace the `AdminRoutingsConfigurator` bullet in Architecture Notes (currently: `AdminRoutingsConfigurator
— registers all /admin/... routes under authenticate { }. Uses requireAdmin() helper (private
RoutingContext extension) to verify caller is root.`) with:

> `AdminRoutingsConfigurator` — registers all `/admin/...` routes under `authenticate { }`. Uses
> `requireAdmin()` helper (private `RoutingContext` extension) to verify caller holds the SuperAdmin role,
> via `simpleRoles.server`'s `SimpleRolesFeature.isSuperAdmin` (issue #68) — replaces the previous inline
> `username == "root"` comparison. `usersRepo: ReadUsersRepo` is kept on the constructor only for the
> unrelated `GET /admin/users/getById/{id}` route.

### `features/email/README.md`

In the Models table, update the `EmailFeatureService` row's description (currently ends "...`sendTestEmail`
enforces root-only access before delegating;...") to "...`sendTestEmail` enforces a SuperAdmin-only check
(via `simpleRoles.server`'s `SimpleRolesFeature`) before delegating;...".

Replace the "Root guard" Architecture Notes bullet (currently: `Root-only enforcement for sendTest happens
inside EmailFeatureService.sendTestEmail by comparing caller.username.string against the literal "root"`)
with:

> **Superadmin guard:** Both `POST /email/sendTest` and `PUT /email/myEmail` use only
> `getCallerUserIdOrAnswerUnauthorized()` at the routing layer (self-service auth — 401 on missing/invalid
> bearer token). Superadmin-only enforcement for `sendTest` happens inside
> `EmailFeatureService.sendTestEmail` by calling `simpleRoles.server`'s `SimpleRolesFeature.isSuperAdmin(callerId)`
> (issue #68) — replaces the previous inline `caller.username.string == "root"` comparison, and also
> removes the separate `usersRepo.getById` lookup that comparison needed (an unknown `UserId` now
> resolves to `isSuperAdmin == false` directly, the same net outcome as the old "caller not found" branch).
> On failure — whether the caller isn't superadmin or the SMTP send itself failed — the route responds
> `500 Internal Server Error` (the two failure modes are indistinguishable at the HTTP layer, unchanged).

### `features/files/README.md`

In the Routes table, update the `PUT /files/avatar/{userId}` row's description (currently "...allowed only
for the user themselves or `root`;...") to "...allowed only for the user themselves or a SuperAdmin;...".

Replace the sentence in the "User avatars" Architecture Notes bullet (currently: `the PUT /files/avatar/{userId}
route authorizes the caller as the user themselves or root (root resolved via ReadUsersRepo injected into
FilesRoutingsConfigurator, username "root");`) with:

> the `PUT /files/avatar/{userId}` route authorizes the caller as the user themselves or a SuperAdmin
> (resolved via `simpleRoles.server`'s `SimpleRolesFeature.isSuperAdmin`, injected into
> `FilesRoutingsConfigurator` — issue #68; the constructor no longer takes `ReadUsersRepo`, which this was
> its only use of);

### `features/ui/users/README.md`

In Overview, replace every "and `root`" / "root may" phrasing (3 bullets: users-list, profile-view,
profile-edit) with "and a SuperAdmin" / "a SuperAdmin may" — wording only, behavior unchanged.

In the Models table, update the `UsersModel` row's parenthetical (currently
"`isCurrentUserRoot`") to "`isCurrentUserRootFlow` — now SuperAdmin-role-backed, see Architecture Notes".

Replace the "Root detection is client-side" Architecture Notes bullet (currently: `Root detection is
client-side (me.value?.username?.string == "root"); the server still enforces root on every admin endpoint
(403) and owner-or-root on the avatar PUT (403).`) with:

> **Superadmin detection is client-side**, via `simpleRoles.client`'s `CacheSimpleRolesFeature.isSuperAdminStateFlow`
> (issue #68) — replaces the previous `me.value?.username?.string == "root"` comparison.
> `UsersModel.isCurrentUserRootFlow` keeps its name (SuperAdmin is architecturally fixed to `root`, so the
> two remain the same boolean) but its Koin wiring now injects `CacheSimpleRolesFeature` directly (not the
> narrower `SimpleRolesFeature` interface) specifically to read this reactive flow — see
> `simpleRoles/README.md` Architecture Notes for why. The server still enforces independently on every
> admin endpoint (`403`) and on the avatar `PUT` (`403`) — unchanged, just re-worded mechanism.

### `features/users/README.md`

Optional, one line added to the end of the "Root user bootstrap happens inside..." Architecture Notes
bullet:

> — see `roles/README.md` for which roles get granted to a newly created user and by what mechanism
> (issue #68).

### `features/auth/README.md`

Optional, same rationale — one line appended after the "Root-user bootstrap" note referenced in Overview
or Architecture Notes:

> Role assignment for newly created/bootstrapped users (issue #68) is handled separately by
> `features/roles` — see `roles/README.md`.

---

## 8. Test stubs — full code (task item 3)

Per `agents/ARCHITECTURE.md`'s Test Planning Requirement, every planned change below has concrete test
code, not just names. Style mirrors this repo's established convention (`kotlin.test` +
`kotlinx-coroutines-test`'s `runTest`; small `Fake*` doubles; no live-DB tests — confirmed again this step
via `grep -rln` across the repo returning nothing for `Database.connect|H2|testcontainers` outside
Exposed-repo implementation code itself). No new `commonTest` Gradle dependencies are needed anywhere —
`kotlin('test-common')`, `kotlin('test-annotations-common')`, and `libs.kotlin.coroutines.test` are already
wired into every module's `commonTest` source set by the `defaultProject.gradle` template (verified by
reading it this step), exactly as `email/server`'s existing tests already rely on with no explicit
declaration.

### 8.1 `features/simpleRoles/server/src/commonTest/kotlin/services/FakeRolesRepo.kt` (fixture, pass 1)

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult

/**
 * Minimal in-memory [ReadRolesRepo] test double: a fixed `BaseRoleSubject -> Set<BaseRole>` map with
 * no hierarchy/transitive-role support (not needed — [SimpleRolesFeatureService] only calls
 * [contains] directly). Module-local, mirroring `email/server`'s `FakeUsersRepo` convention.
 *
 * @param grants Fixed subject-to-roles seed data.
 */
internal class FakeRolesRepo(
    private val grants: Map<BaseRoleSubject, Set<BaseRole>> = emptyMap()
) : ReadRolesRepo {
    override suspend fun getDirectSubjects(role: BaseRole): List<BaseRoleSubject> =
        grants.filterValues { role in it }.keys.toList()

    override suspend fun getDirectRoles(subject: BaseRoleSubject): List<BaseRole> =
        grants[subject]?.toList() ?: emptyList()

    override suspend fun getAll(): Map<BaseRoleSubject, List<BaseRole>> =
        grants.mapValues { it.value.toList() }

    override suspend fun getAllRolesByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRole> {
        val allRoles = grants.values.flatten().distinct().let { if (reversed) it.reversed() else it }
        return allRoles.createPaginationResult(pagination, allRoles.size.toLong())
    }

    override suspend fun getAllSubjectsByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRoleSubject> {
        val allSubjects = grants.keys.toList().let { if (reversed) it.reversed() else it }
        return allSubjects.createPaginationResult(pagination, allSubjects.size.toLong())
    }

    override suspend fun contains(subject: BaseRoleSubject, role: BaseRole): Boolean =
        role in (grants[subject] ?: emptySet())

    override suspend fun containsAny(subject: BaseRoleSubject, roles: List<BaseRole>): Boolean =
        (grants[subject] ?: emptySet()).any { it in roles }
}
```

### 8.2 `features/simpleRoles/server/src/commonTest/kotlin/services/SimpleRolesFeatureServiceTest.kt` (pass 1)

```kotlin
package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.UserRole
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [SimpleRolesFeatureService.isSuperAdmin]'s three cases: directly-granted SuperAdmin,
 * User-only (no SuperAdmin), and a completely unknown/never-granted [UserId].
 */
class SimpleRolesFeatureServiceTest {
    private val superAdminUserId = UserId(1L)
    private val plainUserId = UserId(2L)
    private val unknownUserId = UserId(999L)

    /** A subject with SuperAdmin directly granted → `true`. */
    @Test
    fun isSuperAdminReturnsTrueForSubjectWithSuperAdminRole() = runTest {
        val rolesRepo = FakeRolesRepo(
            mapOf(BaseRoleSubject.Direct(superAdminUserId.long.toString()) to setOf(UserRole, SuperAdminRole))
        )
        val service = SimpleRolesFeatureService(rolesRepo)

        assertTrue(service.isSuperAdmin(superAdminUserId))
    }

    /** A subject with only User (no SuperAdmin) → `false`. */
    @Test
    fun isSuperAdminReturnsFalseForSubjectWithOnlyUserRole() = runTest {
        val rolesRepo = FakeRolesRepo(
            mapOf(BaseRoleSubject.Direct(plainUserId.long.toString()) to setOf(UserRole))
        )
        val service = SimpleRolesFeatureService(rolesRepo)

        assertFalse(service.isSuperAdmin(plainUserId))
    }

    /** An unknown/never-granted [UserId] → `false`. */
    @Test
    fun isSuperAdminReturnsFalseForUnknownUser() = runTest {
        val service = SimpleRolesFeatureService(FakeRolesRepo())

        assertFalse(service.isSuperAdmin(unknownUserId))
    }
}
```

### 8.3 `features/roles/server/src/commonTest/kotlin/FakeRolesRepo.kt` (fixture, pass 2 — Read+Write)

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory [RolesRepo] test double backed by a plain `MutableMap<BaseRoleSubject, MutableSet<BaseRole>>`
 * — no hierarchy/transitive-role support beyond [RolesRepo]'s own default [getAllRoles] (sufficient
 * for this app's flat `SuperAdmin`/`User` roles; see `roles/README.md` Architecture Notes). The two
 * paginated methods ignore true page slicing (no test in this module exercises them) but stay
 * correctly typed so the class compiles as a full [RolesRepo]. Module-local fixture, mirroring
 * `email/server`'s `FakeUsersRepo` convention.
 */
internal class FakeRolesRepo : RolesRepo {
    private val grants = mutableMapOf<BaseRoleSubject, MutableSet<BaseRole>>()

    private val _roleIncluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    override val roleIncluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleIncluded.asSharedFlow()
    private val _roleExcluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    override val roleExcluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleExcluded.asSharedFlow()
    private val _roleCreated = MutableSharedFlow<BaseRole>()
    override val roleCreated: Flow<BaseRole> = _roleCreated.asSharedFlow()
    private val _roleRemoved = MutableSharedFlow<BaseRole>()
    override val roleRemoved: Flow<BaseRole> = _roleRemoved.asSharedFlow()

    override suspend fun getDirectSubjects(role: BaseRole): List<BaseRoleSubject> =
        grants.filterValues { role in it }.keys.toList()

    override suspend fun getDirectRoles(subject: BaseRoleSubject): List<BaseRole> =
        grants[subject]?.toList() ?: emptyList()

    override suspend fun getAll(): Map<BaseRoleSubject, List<BaseRole>> =
        grants.mapValues { it.value.toList() }

    override suspend fun getAllRolesByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRole> {
        val allRoles = grants.values.flatten().distinct().let { if (reversed) it.reversed() else it }
        return allRoles.createPaginationResult(pagination, allRoles.size.toLong())
    }

    override suspend fun getAllSubjectsByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRoleSubject> {
        val allSubjects = grants.keys.toList().let { if (reversed) it.reversed() else it }
        return allSubjects.createPaginationResult(pagination, allSubjects.size.toLong())
    }

    override suspend fun contains(subject: BaseRoleSubject, role: BaseRole): Boolean =
        role in (grants[subject] ?: emptySet())

    override suspend fun containsAny(subject: BaseRoleSubject, roles: List<BaseRole>): Boolean =
        (grants[subject] ?: emptySet()).any { it in roles }

    override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants.getOrPut(subject) { mutableSetOf() }.add(role)
        if (changed) _roleIncluded.emit(subject to role)
        return changed
    }

    override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants[subject]?.remove(role) ?: false
        if (changed) _roleExcluded.emit(subject to role)
        return changed
    }

    override suspend fun createRole(newRole: BaseRole): Boolean = true.also { _roleCreated.emit(newRole) }

    override suspend fun removeRole(role: BaseRole): Boolean {
        var removedAny = false
        grants.values.forEach { removedAny = it.remove(role) || removedAny }
        if (removedAny) _roleRemoved.emit(role)
        return removedAny
    }
}
```

### 8.4 `features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt` (fixture, pass 2)

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] — same composition shape as
 * `email/server`'s `FakeUsersRepo` (a library base class supplies the CRUD surface, including the
 * real `WriteCRUDRepo.newObjectsFlow` this module's reactive-subscription tests exercise directly).
 * No duplicate-username/email enforcement — not exercised by any test in this module.
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {
    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser =
        old.copy(username = newValue.username, email = newValue.email)

    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}
```

### 8.5 `features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt` (pass 2)

Covers: per-user grant rule (root vs. non-root), its idempotency, the one-time-migration body's
correctness and idempotency across repeated runs, and — replicated directly against a real `newObjectsFlow`
using `UnconfinedTestDispatcher` (the standard, non-flaky idiom for asserting a flow-triggered side effect
completes synchronously within the test) — the reactive-subscription pattern `JVMPlugin.startPlugin` wires
up. `JVMPlugin` itself is not booted (would need live Koin/Ktor/Postgres, outside this repo's convention);
this test exercises the exact same subscription shape (`newObjectsFlow.collect { grantDefaultRoles(...) }`)
against the extracted, already-unit-tested `grantDefaultRoles`.

```kotlin
package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.UserRole
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [grantDefaultRoles] (the per-user grant rule and its idempotency), [backfillDefaultRoles]
 * (the one-time migration body and its idempotency across repeated runs), and the reactive-subscription
 * *pattern* [dev.inmo.wishlist.features.roles.server.JVMPlugin.startPlugin] wires up — replicated here
 * against a real [FakeUsersRepo.newObjectsFlow] (the same MicroUtils `MapCRUDRepo`/`WriteMapCRUDRepo`
 * machinery the production `CacheUsersRepo` is ultimately built on) since exercising `JVMPlugin` itself
 * would require a live Koin/Ktor/Postgres boot, outside this repo's unit-test convention.
 */
class RolesBootstrapTest {

    private val rootUser = RegisteredUser(UserId(1L), Username("root"))
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Non-root user → only User is granted, never SuperAdmin. */
    @Test
    fun grantDefaultRolesGrantsOnlyUserRoleForNonRootUser() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, plainUser)

        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertFalse(rolesRepo.contains(subject, SuperAdminRole))
    }

    /** A user named `root` → both User and SuperAdmin are granted. */
    @Test
    fun grantDefaultRolesGrantsUserAndSuperAdminRoleForRootUser() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, rootUser)

        val subject = BaseRoleSubject.Direct(rootUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertTrue(rolesRepo.contains(subject, SuperAdminRole))
    }

    /** Calling [grantDefaultRoles] twice for the same user is a no-op the second time — no error, no duplicate grant. */
    @Test
    fun grantDefaultRolesIsIdempotent() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, rootUser)
        grantDefaultRoles(rolesRepo, rootUser)

        val subject = BaseRoleSubject.Direct(rootUser.id.long.toString())
        assertEquals(
            setOf(UserRole, SuperAdminRole),
            rolesRepo.getDirectRoles(subject).toSet()
        )
    }

    /** [backfillDefaultRoles] grants User to every pre-existing user and SuperAdmin only to `root`. */
    @Test
    fun backfillDefaultRolesGrantsRolesToAllPreExistingUsers() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(rootUser.id to rootUser, plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()

        backfillDefaultRoles(usersRepo, rolesRepo)

        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(rootUser.id.long.toString()), SuperAdminRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(rootUser.id.long.toString()), UserRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(plainUser.id.long.toString()), UserRole))
        assertFalse(rolesRepo.contains(BaseRoleSubject.Direct(plainUser.id.long.toString()), SuperAdminRole))
    }

    /**
     * Running [backfillDefaultRoles] a second time (simulating what a non-version-gated re-run would
     * look like) does not change the outcome — verifies the migration body itself is safe to
     * double-run, independent of whatever gates how many times production actually invokes it
     * (`VersionsRepo.setTableVersion`, not re-tested here — it is already-tested library code with no
     * app-specific branching).
     */
    @Test
    fun backfillDefaultRolesIsIdempotentAcrossRepeatedRuns() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(rootUser.id to rootUser, plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()

        backfillDefaultRoles(usersRepo, rolesRepo)
        val afterFirstRun = rolesRepo.getAll().mapValues { it.value.toSet() }
        backfillDefaultRoles(usersRepo, rolesRepo)
        val afterSecondRun = rolesRepo.getAll().mapValues { it.value.toSet() }

        assertEquals(afterFirstRun, afterSecondRun)
    }

    /**
     * Replicates [dev.inmo.wishlist.features.roles.server.JVMPlugin.startPlugin]'s
     * `usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user -> grantDefaultRoles(rolesRepo, user) }`
     * subscription pattern directly against [FakeUsersRepo.newObjectsFlow]: a user created *after* the
     * subscription starts is granted default roles reactively. Uses [UnconfinedTestDispatcher] so the
     * launched collector coroutine actively subscribes before `create(...)` runs and processes the
     * emission synchronously within the same test step — the standard idiom for testing a
     * flow-triggered side effect deterministically, avoiding a `StandardTestDispatcher` race between
     * "collector scheduled" and "collector actually subscribed."
     */
    @Test
    fun reactiveSubscriptionGrantsDefaultRolesOnNewUserCreation() = runTest(UnconfinedTestDispatcher()) {
        val usersRepo = FakeUsersRepo()
        val rolesRepo = FakeRolesRepo()

        val job = launch {
            usersRepo.newObjectsFlow.collect { user -> grantDefaultRoles(rolesRepo, user) }
        }

        usersRepo.create(listOf(NewUser(Username("root"))))
        usersRepo.create(listOf(NewUser(Username("bob"))))

        val createdRoot = usersRepo.getUserByUsername(Username("root"))!!
        val createdBob = usersRepo.getUserByUsername(Username("bob"))!!

        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdRoot.id.long.toString()), SuperAdminRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdRoot.id.long.toString()), UserRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdBob.id.long.toString()), UserRole))
        assertFalse(rolesRepo.contains(BaseRoleSubject.Direct(createdBob.id.long.toString()), SuperAdminRole))

        job.cancel()
    }
}
```

### 8.6 `features/roles/common/src/commonTest/kotlin/FeatureRolesRegistryTest.kt` (pass 3)

```kotlin
package dev.inmo.wishlist.features.roles.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Verifies [FeatureRolesRegistry]: registration/lookup round-trip, same-role re-registration
 * idempotency, conflicting-role re-registration failure, and the "never registered" `null` case.
 * Every test uses its own unique `featureId` so tests remain independent of execution order within
 * this process-wide singleton.
 */
class FeatureRolesRegistryTest {

    /** `register` then `requiredRole` round-trips the same role. */
    @Test
    fun registerThenRequiredRoleRoundTrips() {
        val featureId = "featureRolesRegistryTest.roundTrip"
        FeatureRolesRegistry.register(featureId, SuperAdminRole)

        assertEquals(SuperAdminRole, FeatureRolesRegistry.requiredRole(featureId))
    }

    /** Re-registering the same id with the same role does not throw. */
    @Test
    fun reRegisteringSameRoleDoesNotThrow() {
        val featureId = "featureRolesRegistryTest.sameRoleTwice"
        FeatureRolesRegistry.register(featureId, UserRole)

        FeatureRolesRegistry.register(featureId, UserRole)

        assertEquals(UserRole, FeatureRolesRegistry.requiredRole(featureId))
    }

    /** Re-registering the same id with a different role throws. */
    @Test
    fun reRegisteringDifferentRoleThrows() {
        val featureId = "featureRolesRegistryTest.conflictingRole"
        FeatureRolesRegistry.register(featureId, UserRole)

        assertFailsWith<IllegalStateException> {
            FeatureRolesRegistry.register(featureId, SuperAdminRole)
        }
    }

    /** A never-registered id resolves to `null`. */
    @Test
    fun requiredRoleForNeverRegisteredIdIsNull() {
        assertNull(FeatureRolesRegistry.requiredRole("featureRolesRegistryTest.neverRegistered"))
    }
}
```

### 8.7 `features/roles/server/src/commonTest/kotlin/utils/RequireRoleTest.kt` (pass 3)

Per `002-planning.md` §7's recommendation (a): the pure decision function is unit-tested directly;
`requireRole` itself (the `RoutingContext` wrapper) has no unit test — this repo has no precedent for
constructing a fake `RoutingContext` (`AuthRoutingsConfigurator`/`AdminRoutingsConfigurator` etc. have no
direct unit tests either; only the pure-logic helpers around them do) — flagged as covered by
build+manual verification only, per the Test Planning Requirement's explicit allowance for this shape of
gap.

```kotlin
package dev.inmo.wishlist.features.roles.server.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.server.FakeRolesRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [isRoleRequirementSatisfied], the pure allow/deny decision behind [requireRole]. Every
 * test registers its own unique feature id into the process-wide [FeatureRolesRegistry] singleton to
 * stay independent of test execution order and of any other test class sharing the same JVM.
 */
class RequireRoleTest {
    private val callerId = UserId(42L)
    private val subject = BaseRoleSubject.Direct(callerId.long.toString())
    private val role = BaseRole("RequireRoleTest.Role")

    /** Registered id + subject holding the required role → allowed. */
    @Test
    fun allowsWhenRegisteredRoleIsHeld() = runTest {
        val featureId = "requireRoleTest.allowed"
        FeatureRolesRegistry.register(featureId, role)
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertTrue(isRoleRequirementSatisfied(featureId, callerId, rolesRepo))
    }

    /** Registered id + subject missing the required role → denied. */
    @Test
    fun deniesWhenRequiredRoleIsMissing() = runTest {
        val featureId = "requireRoleTest.denied"
        FeatureRolesRegistry.register(featureId, role)
        val rolesRepo = FakeRolesRepo()

        assertFalse(isRoleRequirementSatisfied(featureId, callerId, rolesRepo))
    }

    /** Never-registered id → denied without needing any role held (fail-closed on a typo). */
    @Test
    fun deniesWhenFeatureIdWasNeverRegistered() = runTest {
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertFalse(isRoleRequirementSatisfied("requireRoleTest.neverRegistered", callerId, rolesRepo))
    }
}
```

### 8.8 `features/email/server/src/commonTest/kotlin/services/FakeSimpleRolesFeature.kt` (pass 4)

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Fixed-answer [SimpleRolesFeature] test double: [isSuperAdmin] always returns [result] regardless of
 * the queried [UserId]. Records every call's [UserId] in [calls] so tests can assert exactly which
 * caller was checked.
 *
 * @param result Fixed answer every [isSuperAdmin] call returns.
 */
internal class FakeSimpleRolesFeature(
    private val result: Boolean = false
) : SimpleRolesFeature {
    /** Every [UserId] passed to [isSuperAdmin], in call order. */
    val calls = mutableListOf<UserId>()

    override suspend fun isSuperAdmin(userId: UserId): Boolean {
        calls.add(userId)
        return result
    }
}
```

### 8.9 `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` (pass 4 — full re-pointed file)

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] always returns `true` (this
 * class is only ever constructed with a real, non-null `EmailsService` — see `DisabledEmailFeatureTest`
 * for the SMTP-disabled no-op path); [EmailFeatureService.sendTestEmail] delegates the privilege check
 * to `SimpleRolesFeature.isSuperAdmin` before delegating exactly one `EmailsService.sendText` call (the
 * previous "caller not found in `usersRepo`" branch no longer exists as a distinct code path —
 * `sendTestEmail` no longer looks the caller up in `usersRepo` at all, so an unknown `UserId` now takes
 * the same "not superadmin" branch as any other non-superadmin caller); [EmailFeatureService.setMyEmail]
 * persists via `UsersRepo` for a found user, unaffected by the superadmin check.
 */
class EmailFeatureServiceTest {

    /** Fixture user used by every `setMyEmail` assertion (superadmin status is irrelevant there). */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    /** `isFeatureEnabled` unconditionally returns `true` — `emailsService` is a non-nullable constructor parameter, so this class is only ever constructed with a real transport. */
    @Test
    fun isFeatureEnabledAlwaysReturnsTrue() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo(), FakeSimpleRolesFeature())
        assertTrue(service.isFeatureEnabled())
    }

    /** Superadmin caller + present `emailsService` → exactly one `sendText` call with the fixed subject/text; result is `sendText`'s own `true`. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsTrueResult() = runTest {
        val emailsService = FakeEmailsService(result = true)
        val simpleRolesFeature = FakeSimpleRolesFeature(result = true)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), simpleRolesFeature)

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertTrue(result)
        assertEquals(listOf(plainUser.id), simpleRolesFeature.calls)
        assertEquals(1, emailsService.sendTextCalls.size)
        val call = emailsService.sendTextCalls.single()
        assertEquals(recipient, call.recipient)
        assertEquals("Test email from WishlistApp", call.subject)
        assertEquals(
            "This is a test email sent from WishlistApp to verify SMTP configuration.",
            call.text
        )
    }

    /** Same as above but `sendText` fails — the `false` result must propagate through unchanged. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsFalseResult() = runTest {
        val emailsService = FakeEmailsService(result = false)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), FakeSimpleRolesFeature(result = true))

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertFalse(result)
        assertEquals(1, emailsService.sendTextCalls.size)
    }

    /** Non-superadmin caller (including an id unknown to `usersRepo`, now indistinguishable from any other non-superadmin caller) → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseWhenCallerIsNotSuperAdminAndDoesNotCallSendText() = runTest {
        val emailsService = FakeEmailsService()
        val simpleRolesFeature = FakeSimpleRolesFeature(result = false)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), simpleRolesFeature)

        val result = service.sendTestEmail(UserId(999L), recipient)

        assertFalse(result)
        assertEquals(listOf(UserId(999L)), simpleRolesFeature.calls)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** A found user's stored email is updated and persisted via `UsersRepo`, exercising `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by `DisabledEmailFeatureTest`); superadmin status is irrelevant to this method. */
    @Test
    fun setMyEmailPersistsViaUsersRepoForFoundUser() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo, FakeSimpleRolesFeature())
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Caller id resolves to no user → `setMyEmail` returns `false`. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo(), FakeSimpleRolesFeature())

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }

    /** setMyEmail propagates DuplicateUserFieldException, unmodified, when the target email is already stored for a different user. */
    @Test
    fun setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken() = runTest {
        val takenEmail = Email("taken@example.com")
        val ownerUser = plainUser.copy(id = UserId(1L), email = takenEmail)
        val repo = FakeUsersRepo(mapOf(ownerUser.id to ownerUser, plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo, FakeSimpleRolesFeature())

        assertFailsWith<DuplicateUserFieldException> {
            service.setMyEmail(plainUser.id, takenEmail)
        }
    }
}
```

### 8.10 Configurator-level tests — deliberately not added (decision + reasoning)

`002-planning.md` §7 left it to Architecture whether to add net-new `AdminRoutingsConfiguratorTest`/
`FilesRoutingsConfiguratorTest` files. **Decision: do not add them.** Reasoning: neither configurator has
ever had a direct unit test in this codebase (confirmed via `ast-index search`/full reads this step —
routing configurators elsewhere are not unit-tested directly; the logic they call is), and after this
change both `requireAdmin()` and the avatar-`PUT` guard collapse to a two-line delegation to
`SimpleRolesFeature.isSuperAdmin(callerId)` — the only logic left to get wrong is already fully covered by
`SimpleRolesFeatureServiceTest` (§8.2). Inventing a bespoke `RoutingContext`/Ktor test harness that exists
nowhere else in this repo, for two-line delegations with no branching of their own, would add test
scaffolding risk (a novel harness pattern to maintain) without covering any logic the service-level test
doesn't already cover. Coverage for these two sites instead comes from: `SimpleRolesFeatureServiceTest`
(the actual decision logic) + full build + the manual verification steps listed in each Coding pass's
"Verify" section (§4) — consistent with this repo's actual, already-established test-coverage depth.

---

## 9. Consolidated final Coding-pass file list (task item 8)

See §4 for the full pass-by-pass breakdown with rationale. Flat summary, in landing order:

**Pass 1** (30 files): `gradle/libs.versions.toml`, `settings.gradle`, all 12 `roles`+`simpleRoles`
submodule build.gradles/Kotlin stubs listed in §4, `simpleRoles`'s full server+client implementation +
its 2 test files, `client/build.gradle` + 3 client entry points, `server/sample.config.json` +
`server/dev.config.json`, `features/roles/README.md`, `features/simpleRoles/README.md`.

**Pass 2** (6 files): `features/roles/server/build.gradle`, `RolesBootstrap.kt`, `JVMPlugin.kt` (rewrite),
`FakeRolesRepo.kt`, `FakeUsersRepo.kt`, `RolesBootstrapTest.kt`.

**Pass 3** (5 files): `FeatureRolesRegistry.kt`, `roles/common/Plugin.kt` (populate), `FeatureRolesRegistryTest.kt`,
`RequireRole.kt`, `RequireRoleTest.kt`.

**Pass 4** (19 files): the 3 server call-site files + their `Plugin.kt`/`build.gradle` (9 files), 1 client
call-site file + its `build.gradle` (2 files), `EmailFeatureServiceTest.kt` + `FakeSimpleRolesFeature.kt`
(2 files), and the 6 existing-feature READMEs.

Total: 60 file touches across the whole issue (many pass-1 entries are stock-scaffold files with no manual
authoring content, per §3).

---

## 10. Status

**READY for Coding.** No blocking questions remain — both open implementation-style choices from
`002-planning.md` are resolved (§2), every new file has byte-exact Kotlin/Gradle/JSON content verified
against actual library sources (not assumed from the plan's prose), every existing call site has an exact
before/after diff, every planned change has real test code (not stubs-as-names), and the 5-pass proposal
is refined to a concrete, dependency-ordered 4-pass split with an exact file list per pass. The two
sequencing corrections in §1/§4 (README timing, `roles.server.JVMPlugin`'s pass-1 config registration) are
wiring fixes discovered while proving the plan buildable, not new design ambiguity — nothing here should
send this back to Planning.
