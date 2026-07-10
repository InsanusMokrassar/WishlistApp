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
