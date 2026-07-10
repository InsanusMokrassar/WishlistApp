Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~35 minutes wall-clock
Tokens used: not precisely instrumented from inside this agent; rough estimate ~90k-120k tokens (reading `agents/ALL.md`/`local.ALL.md`/`ARCHITECTURE.md`/`CODING.md`, `001-`/`002-planning.md`, every current source file the plan touches, three feature READMEs, and independently re-verifying three library-source claims from `002-planning.md` against the actual version-matched sources jars: `exposed-core-1.3.0-sources.jar` (`ExposedSQLException`'s real package + `getSQLState()`), `exposed-jdbc-1.3.0-sources.jar` (`BlockingExecutable.executeIn`'s `SQLException → ExposedSQLException` wrap site), `micro_utils.repos.exposed-jvm-0.29.4-sources.jar` (`AbstractExposedWriteCRUDRepo`'s open `update`/`create` signatures), and the local `/home/aleksey/projects/own/MicroUtils` checkout's `MapCRUDRepo.kt` for `FakeUsersRepo`'s propagation shape).
Changed files: `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/003-architecturing.md` (this file) only. No source, config, or other doc file was edited by this step (per this task's instruction, this agent must not edit anything else).

---

# Architecturing: formalize `002-planning.md`'s duplicate-key-to-409 plan into exact diffs + test stubs

## 0. Scope and method

`002-planning.md` is READY and already contains a fully investigated, library-source-verified design. My job here is formalization, not redesign: turn every planned change into exact before/after Kotlin text, write full test bodies (not just names) per `agents/ARCHITECTURE.md`'s Test Planning Requirement, make the two explicit calls this task's brief asked for (route-level test coverage sufficiency; shared-helper vs. per-route try/catch), and hand Coding a single authoritative file list.

Before writing anything below I re-read every current source file `002-planning.md` plans to touch (`ExposedUsersRepo.kt`, `WriteUsersRepo.kt`, `CacheUsersRepo.kt`, `UsersRepo.kt`, `EmailFeature.kt`, `EmailFeatureService.kt`, `DisabledEmailFeature.kt`, `UpdateStoredEmail.kt`, `EmailRoutingsConfigurator.kt`, `AdminRoutingsConfigurator.kt`, `UsersManagementFeature.kt`, `FakeUsersRepo.kt`, `EmailFeatureServiceTest.kt`, `DisabledEmailFeatureTest.kt`, and all three READMEs) and confirmed every one matches the "before" state `002-planning.md` assumed — no drift since round 2 was written. I also independently re-verified, against the actual version-matched library sources (not just trusting round 2's citations), the three claims the whole design hinges on:

- **`ExposedSQLException`'s real package.** The file lives at `exposed-core`'s `org/jetbrains/exposed/v1/core/Exceptions.kt` but carries `@file:Suppress("PackageDirectoryMismatch", "InvalidPackageDeclaration")` and actually declares `package org.jetbrains.exposed.v1.exceptions`. So `002-planning.md`'s import `org.jetbrains.exposed.v1.exceptions.ExposedSQLException` is correct.
- **The exact `SQLException → ExposedSQLException` wrap site.** `exposed-jdbc`'s `org/jetbrains/exposed/v1/jdbc/statements/BlockingExecutable.kt`, function `executeIn`, has two `catch (e/cause: SQLException) { throw ExposedSQLException(e, contexts, transaction) }` blocks around the actual JDBC `prepareStatement`/`executeInternal` calls — this is the concrete point where a Postgres unique-violation becomes an `ExposedSQLException`, confirming the exception really does reach `ExposedUsersRepo`'s planned catch site as that type.
- **`getSQLState()` really is exposed as the Kotlin property `sqlState`.** `ExposedSQLException` itself contains `override fun getSQLState(): String = originalSQLException?.sqlState.orEmpty()` — i.e. the Exposed library's own shipped Kotlin source calls `.sqlState` directly on a `java.sql.SQLException`-typed value. This is concrete, in-the-wild proof (not just a plausible-sounding claim) that `internal fun SQLException.isUniqueViolation(): Boolean = sqlState == "23505"` compiles.
- **`AbstractExposedWriteCRUDRepo.update`/`create` are genuinely overridable.** Read the full class: both are plain `override suspend fun` (no `final`), and `AbstractExposedCRUDRepo` (the direct superclass of `ExposedUsersRepo`) re-declares neither. Confirms `ExposedUsersRepo` can add its own `override suspend fun update(id: UserId, value: NewUser): RegisteredUser?` / `create(...)` wrapping `super.update(...)`/`super.create(...)`.
- **`FakeUsersRepo`'s planned `updateObject`/`createObject` throw actually propagates out of `.update()`/`.create()`.** Read `MapCRUDRepo.kt` (local `MicroUtils` checkout, structurally the same shape as the shipped version): `WriteMapCRUDRepo.update()` calls `updateObject(value, id, ...)` directly inside `locker.withWriteLock { }` with no `try`/`catch`; `create()` calls `createObject(it)` the same way. An exception thrown from either fill-in method propagates straight out, uncaught — the same shape as production `ExposedUsersRepo`, confirming the test double is faithful.

All five checks confirm `002-planning.md`'s design compiles and behaves exactly as claimed. **Nothing in the design changes as a result of this verification pass** — I adopt `002-planning.md`'s diffs as final wherever it already spelled them out verbatim, and only add the parts it explicitly deferred (READMEs' exact prose, the three §4.6 "optional" KDocs, `DisabledEmailFeatureTest`'s new test body, the two explicit decisions the calling brief asked for).

---

## 1. Exact final file contents

### 1.1 `features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt` — new file

Package `dev.inmo.wishlist.features.users.common.repo.exceptions` (folder `repo/exceptions/`, matches). Full content, adopted verbatim from `002-planning.md` §3.1 (verified: `RuntimeException` needs no import — `kotlin.RuntimeException` is in the auto-imported `kotlin` package):

```kotlin
package dev.inmo.wishlist.features.users.common.repo.exceptions

/**
 * Thrown by [dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo]'s write operations
 * (`update`, `create`) when the underlying storage rejects the write because a
 * unique-constrained `users` column (`username` or `email`) already holds the given value for a
 * different user.
 *
 * Thrown from the JVM-only Exposed implementation (`ExposedUsersRepo`, the only [WriteUsersRepo]
 * that talks to a real, constraint-enforcing Postgres database) after it translates a caught
 * unique-violation `ExposedSQLException` (SQL state `23505`). Propagates unchanged through
 * `CacheUsersRepo` — the `FullCRUDCacheRepo` write wrapper it is built on does not catch
 * exceptions from the wrapped repo, it only reacts to a successful, non-throwing result.
 *
 * This is the repo-wide convention for signalling "duplicate key" from any [WriteUsersRepo]
 * write: callers that need to distinguish it from other failures — most commonly HTTP route
 * handlers that should respond `409 Conflict` instead of the generic `500 Internal Server Error`
 * an unmapped exception would otherwise produce (Ktor's engine-level
 * `DefaultEnginePipeline.handleFailure` fallback) — catch this type.
 *
 * @param cause The underlying driver exception, if any (kept for logging).
 */
class DuplicateUserFieldException(cause: Throwable? = null) :
    RuntimeException("A user with the same username or email already exists.", cause)
```

### 1.2 `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` — mandatory, full new file content

Confirmed current file matches round 1's baseline exactly (`emailColumn` currently `text("email").nullable()`, no `.uniqueIndex()`, no `update`/`create` override, no `isUniqueViolation`). Adopted verbatim from `002-planning.md` §4.2 — verified compiles logically against the confirmed-open `update(id: IdType, value: InputValueType): ObjectType?` / `create(values: List<InputValueType>): List<ObjectType>` base signatures:

```kotlin
package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLException

/**
 * Exposed-backed implementation of [UsersRepo].
 *
 * Stores users in the `users` table with an auto-increment `id`, a unique `username`, and a
 * unique, nullable `email` column. The nullable `email` column is additive — `initTable()` adds
 * it to any existing table via `createMissingTablesAndColumns`, keeping existing rows valid with
 * `NULL`; `NULL` values are exempt from the uniqueness check, so users without a stored email
 * never collide with each other.
 *
 * [update] and [create] translate a Postgres unique-violation on either unique column into
 * [DuplicateUserFieldException] (see [isUniqueViolation]) instead of letting the raw
 * [ExposedSQLException] escape — callers that need to distinguish "duplicate" from other
 * failures (e.g. HTTP route handlers responding `409 Conflict`) should catch that type.
 *
 * @param database Exposed [Database] instance (provided by the common server plugin).
 */
class ExposedUsersRepo(
    override val database: Database
) : UsersRepo, AbstractExposedCRUDRepo<RegisteredUser, UserId, NewUser>(tableName = "users") {
    /** Auto-increment primary key column. */
    private val idColumn = long("id").autoIncrement()

    /** Unique login name column. */
    private val usernameColumn = text("username").uniqueIndex()

    /**
     * Optional email address column.
     *
     * Nullable so that existing rows (without the column) are treated as `NULL` after the
     * schema migration run by [initTable] / `createMissingTablesAndColumns`. Unique so that
     * two users cannot share the same non-null email address — `NULL` values are exempt from
     * the uniqueness check under standard SQL unique-index semantics, so users without a
     * stored email never collide with each other. A collision surfaces as
     * [DuplicateUserFieldException] from [update]/[create], not a raw driver exception.
     */
    private val emailColumn = text("email").nullable().uniqueIndex()

    override val primaryKey = PrimaryKey(idColumn)

    /**
     * Maps a result row to a [RegisteredUser].
     *
     * Uses [Email.parse] defensively to avoid throwing on any legacy or manually-inserted
     * malformed rows — invalid stored values are treated as absent (`null`).
     */
    override val ResultRow.asObject: RegisteredUser
        get() = RegisteredUser(
            id = UserId(get(idColumn)),
            username = Username(get(usernameColumn)),
            email = get(emailColumn)?.let { Email.parse(it).getOrNull() }
        )

    /** Maps a result row to a [UserId]. */
    override val ResultRow.asId: UserId
        get() = UserId(get(idColumn))

    override val selectById: (UserId) -> Op<Boolean> = { idColumn.eq(it.long) }

    /**
     * Updates mutable columns for an existing or being-inserted user.
     *
     * @param id Ignored during insert; set to the target id on explicit update.
     * @param value Replacement user data.
     * @param it Exposed update/insert builder.
     */
    override fun update(id: UserId?, value: NewUser, it: UpdateBuilder<Int>) {
        it[usernameColumn] = value.username.string
        it[emailColumn] = value.email?.string
    }

    /**
     * Constructs a [RegisteredUser] from an insert statement result, capturing the generated id.
     *
     * @param value The [NewUser] that was inserted.
     * @return [RegisteredUser] with the auto-generated [UserId].
     */
    override fun InsertStatement<Number>.asObject(value: NewUser): RegisteredUser =
        RegisteredUser(
            id = UserId(this[idColumn]),
            username = value.username,
            email = value.email
        )

    /**
     * Looks up a user by their unique [username].
     *
     * @param username Login name to search for.
     * @return Matching [RegisteredUser], or `null` when not found.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        transaction(db = database) {
            selectAll().where { usernameColumn eq username.string }.limit(1).firstOrNull()?.asObject
        }

    /**
     * Persists [value] over the row identified by [id].
     *
     * Wraps the library default (`AbstractExposedWriteCRUDRepo.update`) with translation of a
     * Postgres unique-violation on [usernameColumn]/[emailColumn] into
     * [DuplicateUserFieldException] — every other exception, and the plain `null` return for
     * "no such id", are unchanged.
     *
     * @param id Target user id.
     * @param value Replacement user data.
     * @return Updated [RegisteredUser], or `null` when [id] does not exist.
     * @throws DuplicateUserFieldException when [value]'s username or email is already used by a
     *   different user.
     */
    override suspend fun update(id: UserId, value: NewUser): RegisteredUser? =
        try {
            super.update(id, value)
        } catch (e: ExposedSQLException) {
            if (e.isUniqueViolation()) throw DuplicateUserFieldException(cause = e) else throw e
        }

    /**
     * Inserts [values] as new users.
     *
     * Wraps the library default (`AbstractExposedWriteCRUDRepo.create`) with translation of a
     * Postgres unique-violation on [usernameColumn]/[emailColumn] into
     * [DuplicateUserFieldException].
     *
     * @param values New users to insert.
     * @return Inserted [RegisteredUser]s with generated ids.
     * @throws DuplicateUserFieldException when any of [values]' usernames or emails collides with
     *   an existing user.
     */
    override suspend fun create(values: List<NewUser>): List<RegisteredUser> =
        try {
            super.create(values)
        } catch (e: ExposedSQLException) {
            if (e.isUniqueViolation()) throw DuplicateUserFieldException(cause = e) else throw e
        }

    init {
        initTable()
    }
}

/**
 * Returns whether this exception represents a Postgres `unique_violation` (SQL state `23505`) —
 * i.e. a `.uniqueIndex()`-constrained column already holds the given value for a different row.
 *
 * A standalone, pure function (rather than inlined in [ExposedUsersRepo]'s catch blocks) so it
 * can be unit-tested directly against a plain [SQLException], without a live database.
 */
internal fun SQLException.isUniqueViolation(): Boolean = sqlState == "23505"
```

### 1.3 `features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt` — mandatory KDoc addition

`002-planning.md` §4.6 left this "optional/recommended, not blocking." **Decision: promote to mandatory.** The calling brief for this step explicitly names `WriteUsersRepo` among the files whose `@throws` KDoc I must formalize with exact before/after text, and `agents/CODING.md`'s KDoc Requirements read as a blanket rule ("every `class`, `interface`, `object`, `fun`, `val`/`var` at class/interface level must have a KDoc comment") rather than one scoped only to brand-new files. This file currently has zero KDoc and is directly on the new exception's propagation path, so documenting it costs nothing and closes a real gap while the module is already being touched for this task.

Before:
```kotlin
package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser>
```

After:
```kotlin
package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Write-only surface of the users CRUD repository.
 *
 * The only production implementation whose writes can fail on a constraint collision is the
 * JVM-only [dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo] (reached through
 * [CacheUsersRepo]): its `update`/`create` throw
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] when the
 * given username or non-null email already belongs to a different user. See that exception's
 * KDoc for the full propagation path.
 */
interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser>
```

### 1.4 `features/users/common/src/commonMain/kotlin/repo/CacheUsersRepo.kt` — mandatory KDoc addition

Same promotion-to-mandatory rationale as §1.3. Round 2 §2.3 already confirmed (and I independently re-confirmed by reading `WriteMapCRUDRepo`/`FullCRUDCacheRepo`'s shape) that this file needs **zero code changes** — only KDoc. Before/after; code body is unchanged, only the missing class KDoc and the `getUserByUsername` override's missing KDoc are added (the latter is a small extra beyond round 2's literal ask, included because it's a one-line gap in the same class KDoc-review pass and `agents/CODING.md` requires KDoc on every class-level `fun`):

Before (full current file):
```kotlin
package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.CRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.cache.KVCache
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

class CacheUsersRepo(
    private val originalRepo: UsersRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<UserId, RegisteredUser> = MapKeyValueRepo<UserId, RegisteredUser>(),
    locker: SmartRWLocker = SmartRWLocker()
) : UsersRepo, FullCRUDCacheRepo<RegisteredUser, UserId, NewUser>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredUser::id
) {
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        originalRepo.getUserByUsername(username)
}
```

After (imports and code body byte-identical; only KDocs added):
```kotlin
package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.CRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.cache.KVCache
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

/**
 * [FullCRUDCacheRepo]-backed [UsersRepo] wrapper around [originalRepo].
 *
 * Reads are served from an in-memory cache (pre-filled from [originalRepo] on startup since
 * `skipStartInvalidate = false`); writes delegate to [originalRepo] and update the cache only on
 * a successful, non-throwing result. In particular, [FullCRUDCacheRepo]'s write wrapper does
 * **not** catch exceptions from [originalRepo] — a
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] thrown by
 * the JVM `ExposedUsersRepo` on a unique-constraint violation propagates through this class
 * unchanged, and the cache is left untouched (no partial/incorrect write is ever cached).
 *
 * @param originalRepo Backing [UsersRepo], normally the JVM-only Exposed implementation.
 * @param scope Coroutine scope the cache uses for its background invalidation subscriptions.
 * @param kvCache In-memory key-value store backing the cache; defaults to a plain [MapKeyValueRepo].
 * @param locker Read/write lock guarding concurrent cache access.
 */
class CacheUsersRepo(
    private val originalRepo: UsersRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<UserId, RegisteredUser> = MapKeyValueRepo<UserId, RegisteredUser>(),
    locker: SmartRWLocker = SmartRWLocker()
) : UsersRepo, FullCRUDCacheRepo<RegisteredUser, UserId, NewUser>(
    crudRepo = originalRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredUser::id
) {
    /**
     * Looks up a user by [username] directly against [originalRepo] — bypassing the id-keyed
     * cache, which has no username index.
     *
     * @param username Login name to search for.
     * @return Matching [RegisteredUser], or `null` when not found.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        originalRepo.getUserByUsername(username)
}
```

### 1.5 `features/email/server/src/commonMain/kotlin/EmailFeature.kt` — mandatory KDoc addition

Before (relevant declaration only; rest of file unchanged):
```kotlin
    /**
     * Updates or clears the email address stored for [callerId].
     *
     * Self-service — no elevated privilege required. Returns `false` when [callerId] is not found.
     *
     * @param callerId Authenticated caller whose email address is being changed.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found or the
     *   update failed.
     */
    suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean
```

After:
```kotlin
    /**
     * Updates or clears the email address stored for [callerId].
     *
     * Self-service — no elevated privilege required. Returns `false` when [callerId] is not found.
     *
     * @param callerId Authenticated caller whose email address is being changed.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found or the
     *   update failed.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [email] is already stored for a different user.
     */
    suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean
```

### 1.6 `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — mandatory KDoc addition

Before (relevant declaration only):
```kotlin
    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical to [DisabledEmailFeature.setMyEmail].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
```

After:
```kotlin
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
```

### 1.7 `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt` — mandatory KDoc addition

Before (relevant declaration only):
```kotlin
    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical behavior to [EmailFeatureService.setMyEmail],
     * since storage is independent of SMTP configuration.
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
```

After:
```kotlin
    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical behavior to [EmailFeatureService.setMyEmail],
     * since storage is independent of SMTP configuration.
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
```

### 1.8 `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt` — mandatory KDoc addition

Before (full current file):
```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Updates or clears the stored email address for [callerId] via [usersRepo].
 *
 * Shared by [EmailFeatureService.setMyEmail] and [DisabledEmailFeature.setMyEmail] — per-user
 * email-address storage is intentionally independent of SMTP configuration (see
 * `features/email/README.md`), so both [dev.inmo.wishlist.features.email.server.EmailFeature]
 * implementations must persist through this identical path.
 *
 * @param usersRepo User repository used to look up and update the caller's record.
 * @param callerId User whose record is updated.
 * @param email New address to store, or `null` to clear the current address.
 * @return `true` when the update was persisted; `false` when the user was not found.
 */
internal suspend fun updateStoredEmail(usersRepo: UsersRepo, callerId: UserId, email: Email?): Boolean {
    val user = usersRepo.getById(callerId) ?: return false
    return usersRepo.update(callerId, NewUser(user.username, email)) != null
}
```

After (only the KDoc block changes; function body unchanged):
```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Updates or clears the stored email address for [callerId] via [usersRepo].
 *
 * Shared by [EmailFeatureService.setMyEmail] and [DisabledEmailFeature.setMyEmail] — per-user
 * email-address storage is intentionally independent of SMTP configuration (see
 * `features/email/README.md`), so both [dev.inmo.wishlist.features.email.server.EmailFeature]
 * implementations must persist through this identical path.
 *
 * @param usersRepo User repository used to look up and update the caller's record.
 * @param callerId User whose record is updated.
 * @param email New address to store, or `null` to clear the current address.
 * @return `true` when the update was persisted; `false` when the user was not found.
 * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
 *   when [email] is already stored for a different user. This function does not catch it, by
 *   design — [dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator]
 *   catches it at the HTTP boundary and responds `409 Conflict`.
 */
internal suspend fun updateStoredEmail(usersRepo: UsersRepo, callerId: UserId, email: Email?): Boolean {
    val user = usersRepo.getById(callerId) ?: return false
    return usersRepo.update(callerId, NewUser(user.username, email)) != null
}
```

### 1.9 `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — mandatory KDoc addition

Before (relevant declarations only; `getAll`, `setPassword`, `delete` unchanged and omitted for brevity — `setPassword`/`delete` already have KDoc today, `getAll` does not and stays undocumented since it is untouched by this task's exception change):
```kotlin
    suspend fun create(newUserWithPassword: NewUserWithPassword): RegisteredUser? {
        val user = usersRepo.create(NewUser(newUserWithPassword.username)).firstOrNull() ?: return null
        authService.setPassword(user.id, newUserWithPassword.password)
        return user
    }

    suspend fun update(id: UserId, newUser: NewUser): Boolean? {
        if (!usersRepo.contains(id)) return null
        return usersRepo.update(id, newUser) != null
    }
```

After:
```kotlin
    /**
     * Creates a new user with a hashed password.
     *
     * @param newUserWithPassword Desired username, plus plaintext password (hashed via
     *   [authService] before storage).
     * @return The newly created [RegisteredUser], or `null` when creation failed.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [newUserWithPassword]'s username is already taken by another user.
     */
    suspend fun create(newUserWithPassword: NewUserWithPassword): RegisteredUser? {
        val user = usersRepo.create(NewUser(newUserWithPassword.username)).firstOrNull() ?: return null
        authService.setPassword(user.id, newUserWithPassword.password)
        return user
    }

    /**
     * Replaces the stored username/email of user [id].
     *
     * @param id User to update.
     * @param newUser Replacement username/email pair.
     * @return `true` when the update was persisted; `null` when no such user exists.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [newUser]'s username or email is already taken by another user.
     */
    suspend fun update(id: UserId, newUser: NewUser): Boolean? {
        if (!usersRepo.contains(id)) return null
        return usersRepo.update(id, newUser) != null
    }
```

---

## 2. Test stubs (full bodies), per `agents/ARCHITECTURE.md`'s Test Planning Requirement

### 2.1 `IsUniqueViolationTest` — new file, pure-function unit test

`features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` — **new file**, and the first `jvmTest` source set anywhere in this repo (every existing test elsewhere lives in a single-JVM-target `mppJavaProject` module's `commonTest`, which is the JVM compilation directly; `features/users/common` is genuinely multiplatform (`mppJvmJsAndroid`), so a JVM-only test using `java.sql.SQLException` must go in `jvmTest`, not `commonTest`, or it would fail to compile for the `js`/`android` targets). Confirmed both source sets are already wired with no `build.gradle` change needed: `defaultProject.gradle` wires `commonTest` with `kotlin-test-common`/`test-annotations-common`/coroutines-test; `enableMPPJvm.gradle` additionally wires `jvmTest { implementation kotlin('test-junit') }` unconditionally for every JVM-enabled MPP module. Full content, adopted verbatim from `002-planning.md` §5.1:

```kotlin
package dev.inmo.wishlist.features.users.common.repo

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies [isUniqueViolation]'s SQL-state matching in isolation, without a live database. */
class IsUniqueViolationTest {
    @Test
    fun returnsTrueForPostgresUniqueViolationSqlState() {
        assertTrue(SQLException("duplicate key value violates unique constraint", "23505").isUniqueViolation())
    }

    @Test
    fun returnsFalseForOtherSqlStates() {
        assertFalse(SQLException("foreign key violation", "23503").isUniqueViolation())
    }

    @Test
    fun returnsFalseWhenSqlStateIsNull() {
        assertFalse(SQLException("generic failure").isUniqueViolation())
    }
}
```

`isUniqueViolation` is `internal` in `ExposedUsersRepo.kt`; visible here because Kotlin's default JVM test compilation is associated with the main compilation (friend-module access), the same mechanism every other `internal`-testing pattern in this repo already relies on.

### 2.2 `FakeUsersRepo` — full upgraded file (enforces uniqueness so propagation is testable)

`features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` — full new content. Verified against the current fixtures in both consuming test files (`EmailFeatureServiceTest`/`DisabledEmailFeatureTest`, read in full): every existing scenario uses distinct usernames (`"root"`/`"alice"`) and non-colliding emails, and the `it.id != id` exclusion means a user updating their own already-stored username/email is correctly *not* treated as a collision — no existing test breaks:

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException

/**
 * In-memory [UsersRepo] test double backed by [MapCRUDRepo] (`dev.inmo:micro_utils.repos.inmemory`,
 * already on this module's test classpath transitively via `features/common/common`'s
 * `api libs.microutils.repos.cache` dependency — no new `build.gradle` entry needed). Mirrors the
 * composition shape of the production [dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo]:
 * a library base class supplies the [dev.inmo.micro_utils.repos.CRUDRepo] surface, [getUserByUsername]
 * is added by hand.
 *
 * Enforces the same uniqueness rule the production
 * [dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo] enforces at the database level:
 * [updateObject] and [createObject] throw [DuplicateUserFieldException] when [NewUser.username] or a
 * non-null [NewUser.email] already belongs to a different stored user — a record never collides with
 * itself. This makes the fake a faithful double for exercising the duplicate-key-to-409 propagation
 * contract at the service layer without a live database.
 *
 * IDs are assigned sequentially starting one past the highest seeded [UserId]. [getUserByUsername]
 * performs a linear scan via `getAll()` — acceptable for the small fixtures used in these tests.
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : UsersRepo, MapCRUDRepo<RegisteredUser, UserId, NewUser>(initialUsers.toMutableMap()) {

    /** Next id assigned by [createObject], one past the highest id in the seeded map. */
    private var nextId: Long = (initialUsers.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    /**
     * Applies [newValue] on top of [old], keeping [old]'s id.
     *
     * @param newValue New username/email pair to apply.
     * @param id Id of the record being updated (used to exclude self from the uniqueness check).
     * @param old Current stored record.
     * @return Updated record.
     * @throws DuplicateUserFieldException when [newValue]'s username or non-null email already
     *   belongs to a different stored user.
     */
    override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser {
        if (getAll().values.any { it.id != id && it.username == newValue.username }) {
            throw DuplicateUserFieldException()
        }
        if (newValue.email != null && getAll().values.any { it.id != id && it.email == newValue.email }) {
            throw DuplicateUserFieldException()
        }
        return old.copy(username = newValue.username, email = newValue.email)
    }

    /**
     * Assigns the next sequential [UserId] and builds a [RegisteredUser] from [newValue].
     *
     * @param newValue Username/email pair to persist.
     * @return The assigned id paired with the newly registered user.
     * @throws DuplicateUserFieldException when [newValue]'s username or non-null email already
     *   belongs to a stored user.
     */
    override suspend fun createObject(newValue: NewUser): Pair<UserId, RegisteredUser> {
        if (getAll().values.any { it.username == newValue.username }) {
            throw DuplicateUserFieldException()
        }
        if (newValue.email != null && getAll().values.any { it.email == newValue.email }) {
            throw DuplicateUserFieldException()
        }
        val id = UserId(nextId++)
        return id to RegisteredUser(id, newValue.username, newValue.email)
    }

    /**
     * Linear scan over all stored users for one matching [username].
     *
     * @param username Username to look up.
     * @return Matching user, or `null` when none is stored.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}
```

### 2.3 `EmailFeatureServiceTest` — new test method

Add to the existing class (adopted verbatim from `002-planning.md` §5.3; new imports `kotlin.test.assertFailsWith` and `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`):

```kotlin
    /** setMyEmail propagates DuplicateUserFieldException, unmodified, when the target email is already stored for a different user. */
    @Test
    fun setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken() = runTest {
        val takenEmail = Email("taken@example.com")
        val ownerUser = rootUser.copy(email = takenEmail)
        val repo = FakeUsersRepo(mapOf(ownerUser.id to ownerUser, plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo)

        assertFailsWith<DuplicateUserFieldException> {
            service.setMyEmail(plainUser.id, takenEmail)
        }
    }
```

(Verified `assertFailsWith<T>(block: () -> Unit)` is `inline` with a non-`crossinline`/non-`noinline` lambda parameter — inlining splices the block into the caller, so calling the `suspend` `service.setMyEmail(...)` inside it from within `runTest { }`'s suspend context is legal Kotlin, the same reason `measureTimeMillis { delay(...) } ` compiles from a suspend function.)

### 2.4 `DisabledEmailFeatureTest` — new test method

`002-planning.md` §5.3 described this only as "the analogous test... same fixtures, same assertion shape" without spelling out the body. Full body (mirrors §2.3 exactly, against `DisabledEmailFeature` instead of `EmailFeatureService`, matching this file's existing "storage stays independent of SMTP" theme):

```kotlin
    /** setMyEmail propagates DuplicateUserFieldException, unmodified, when the target email is already stored for a different user — the SMTP-disabled path behaves identically to EmailFeatureService's. */
    @Test
    fun setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken() = runTest {
        val takenEmail = Email("taken@example.com")
        val ownerUser = rootUser.copy(email = takenEmail)
        val repo = FakeUsersRepo(mapOf(ownerUser.id to ownerUser, plainUser.id to plainUser))
        val feature = DisabledEmailFeature(repo)

        assertFailsWith<DuplicateUserFieldException> {
            feature.setMyEmail(plainUser.id, takenEmail)
        }
    }
```

New imports for this file: `kotlin.test.assertFailsWith`, `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

---

## 3. Route-level test coverage — explicit decision (per this task's brief)

**Decision: the layered coverage above (§2.1-§2.4) is sufficient. I do not recommend building a Ktor route-test harness, and I do not recommend a shared `respondOnDuplicateOrElse`-style helper, for this task.**

Justification:

- **What the new coverage actually exercises.** `IsUniqueViolationTest` covers the one genuinely new piece of *logic* (`isUniqueViolation`'s SQL-state predicate) in complete isolation. `FakeUsersRepo` + the two new service-layer tests exercise the full propagation contract from a write call down at the `UsersRepo` interface boundary up through `EmailFeatureService`/`DisabledEmailFeature` — i.e. everything up to, but not including, the three-line `try { ... } catch (e: DuplicateUserFieldException) { call.respond(HttpStatusCode.Conflict); return@X }` block in each route handler. What's left untested is exactly the mechanical "catch this one type, respond 409, return" translation and the real Postgres `ExposedSQLException` throw, neither of which contains any branching logic beyond what's already covered.
- **No infrastructure exists to build on, repo-wide, for any feature.** Confirmed (repeating and re-confirming round 2's own grep): zero `testApplication`/`ApplicationTestBuilder` usage anywhere in this codebase, and `features/admin/server` has zero test files today. Building either would be standing up new, cross-cutting test infrastructure that no other feature in this repo has — a materially larger, separate investment than "add a duplicate-key 409," not a natural extension of this task's scope.
- **The untested residual risk is small and mechanical.** Each route's new code is a straight try/catch translating one exception type to one status code, wrapped tightly around exactly the call that can throw (not the whole handler body). This shape is simple enough that build-time type-checking plus the mandated manual/build verification (§6 below) catches the realistic failure modes (wrong import, wrong status code, catching too broad/narrow a scope) at negligible cost compared to writing and maintaining a first-ever Ktor test harness.
- **This matches, rather than deviates from, established repo precedent.** `features/email/README.md` already documents "the live-SMTP success path is intentionally not unit-tested (external integration)" for the exact same class of reasoning — DB/network-boundary behavior in this repo is verified by build + manual check, not by a purpose-built integration-test harness, and this task does not carry a strong enough independent reason to be the one to change that repo-wide convention.
- **No shared "duplicate → 409" helper.** The three call sites have different return-type shapes (`Boolean`, `Boolean?`, `RegisteredUser?`) and different Ktor route builders (`put`/`put`/`post`), so a generic wrapper would need either a functional parameter per call site (as much code as the duplication it removes) or a shape-erasing abstraction that makes each site *harder* to read than the inline three-liner it replaces. Concurring with `002-planning.md` §3.3: keep the three inline `try`/`catch` blocks.

This is a **flagged, accepted gap**, not a silent omission — carried forward explicitly per `agents/ARCHITECTURE.md`'s Test Planning Requirement:
- Route-level HTTP assertions (`EmailRoutingsConfigurator`/`AdminRoutingsConfigurator` actually returning `409`) — not automated this round; verified only by module build + optional manual check (§6).
- `UsersManagementFeature`-level unit tests (`admin/server`, which has no test harness at all today) — not automated this round; the equivalent propagation contract is exercised one layer down via `IsUniqueViolationTest` + the email-service-layer tests.
- `ExposedUsersRepo.update`/`create`'s `try`/`catch` wiring against a real Postgres unique-violation — not unit-testable without a live database; no live-DB test harness exists anywhere in this repo today.

If the operator disagrees with this call, the concrete alternative is scoped and cheap to name: add `ktor-server-test-host` to `features/email/server`'s and `features/admin/server`'s `build.gradle` (`testImplementation`) and write `testApplication { ... }` blocks around each of the three routes — but this is new infrastructure, not a "finish the plan" action, so I am not doing it unilaterally.

---

## 4. Exact route-handler diffs (3 handlers)

All three confirmed against the actual current source (read in full above) — byte-for-byte match with `002-planning.md` §4.3/§4.4's "before" blocks, so these diffs are adopted verbatim as final.

### 4.1 `PUT /email/myEmail` — `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt`

New import: `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

Class KDoc — before:
```kotlin
/**
 * Ktor routing configurator for the email feature.
 *
 * Registers three endpoints under the `/email` path prefix (auto-prefixed to `/api/email` by the
 * server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET  /email/enabled`   — public; returns whether SMTP delivery is configured.
 * - `POST /email/sendTest`  — bearer; the caller identity is passed to [feature] which enforces
 *   root-only access.
 * - `PUT  /email/myEmail`   — bearer (self-service); the caller identity is passed to [feature]
 *   which persists the address.
 *
 * The public `enabled` probe lives outside the `authenticate { }` block so callers without a
 * token can still check availability. Authorization and persistence logic reside entirely in
 * [feature] — this configurator only extracts the caller identity and delegates.
 *
 * @param feature Server-side [EmailFeature] implementation that enforces access rules and
 *   handles email-address persistence.
 */
```

Class KDoc — after:
```kotlin
/**
 * Ktor routing configurator for the email feature.
 *
 * Registers three endpoints under the `/email` path prefix (auto-prefixed to `/api/email` by the
 * server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET  /email/enabled`   — public; returns whether SMTP delivery is configured.
 * - `POST /email/sendTest`  — bearer; the caller identity is passed to [feature] which enforces
 *   root-only access.
 * - `PUT  /email/myEmail`   — bearer (self-service); the caller identity is passed to [feature]
 *   which persists the address, responding `409 Conflict` when the address is already stored for
 *   a different user
 *   (see [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException]).
 *
 * The public `enabled` probe lives outside the `authenticate { }` block so callers without a
 * token can still check availability. Authorization and persistence logic reside entirely in
 * [feature] — this configurator only extracts the caller identity and delegates.
 *
 * @param feature Server-side [EmailFeature] implementation that enforces access rules and
 *   handles email-address persistence.
 */
```

`put(EmailConstants.myEmailPathPart)` handler — before:
```kotlin
                put(EmailConstants.myEmailPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val request = call.receive<SetEmailRequest>()
                    val updated = feature.setMyEmail(callerId, request.email)
                    when {
                        updated -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
```

After:
```kotlin
                put(EmailConstants.myEmailPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val request = call.receive<SetEmailRequest>()
                    val updated = try {
                        feature.setMyEmail(callerId, request.email)
                    } catch (e: DuplicateUserFieldException) {
                        call.respond(HttpStatusCode.Conflict)
                        return@put
                    }
                    when {
                        updated -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
```

`sendTest`/`enabled` handlers unchanged — neither writes a unique-constrained column.

### 4.2 `POST /admin/users/create` and `PUT /admin/users/update/{id}` — `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt`

New import: `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

Class KDoc — before:
```kotlin
/**
 * Ktor routing configurator that registers all admin CRUD endpoints under `/admin`.
 *
 * All routes require a valid bearer token AND the authenticated caller must be the `root` user.
 * Non-root callers receive `403 Forbidden`.
 *
 * **Users management routes** (`/admin/users/...`):
 * - `GET    /admin/users/getAll`         — list all registered users
 * - `POST   /admin/users/create`         — create user with password; body: [NewUserWithPassword]
 * - `PUT    /admin/users/update/{id}`    — update user info; body: [NewUser]
 * - `DELETE /admin/users/delete/{id}`    — remove user by id
 *
 * **Wishlists management routes** (`/admin/wishlists/...`):
 * - `GET    /admin/wishlists/getByUserId/{userId}` — get wishlists owned by a specific user
 * - `GET    /admin/wishlists/getById/{id}`         — get a single wishlist by id
 * - `POST   /admin/wishlists/create`               — create wishlist for any user; body: [NewWishlist]
 * - `PUT    /admin/wishlists/update/{id}`           — update any wishlist (no ownership check); body: [NewWishlistInFeature]
 * - `DELETE /admin/wishlists/delete/{id}`           — delete any wishlist (no ownership check)
 *
 * Wishlist read operations delegate to [WishlistService] (existing functionality).
 * Wishlist write operations that bypass ownership delegate directly to [WishlistRepo] (existing functionality).
 */
```

Class KDoc — after (two bullet lines gain a `409` note; a closing sentence names the exception, mirroring `EmailRoutingsConfigurator`'s treatment):
```kotlin
/**
 * Ktor routing configurator that registers all admin CRUD endpoints under `/admin`.
 *
 * All routes require a valid bearer token AND the authenticated caller must be the `root` user.
 * Non-root callers receive `403 Forbidden`.
 *
 * **Users management routes** (`/admin/users/...`):
 * - `GET    /admin/users/getAll`         — list all registered users
 * - `POST   /admin/users/create`         — create user with password; body: [NewUserWithPassword]; `409` on duplicate username
 * - `PUT    /admin/users/update/{id}`    — update user info; body: [NewUser]; `409` on duplicate username/email
 * - `DELETE /admin/users/delete/{id}`    — remove user by id
 *
 * **Wishlists management routes** (`/admin/wishlists/...`):
 * - `GET    /admin/wishlists/getByUserId/{userId}` — get wishlists owned by a specific user
 * - `GET    /admin/wishlists/getById/{id}`         — get a single wishlist by id
 * - `POST   /admin/wishlists/create`               — create wishlist for any user; body: [NewWishlist]
 * - `PUT    /admin/wishlists/update/{id}`           — update any wishlist (no ownership check); body: [NewWishlistInFeature]
 * - `DELETE /admin/wishlists/delete/{id}`           — delete any wishlist (no ownership check)
 *
 * Wishlist read operations delegate to [WishlistService] (existing functionality).
 * Wishlist write operations that bypass ownership delegate directly to [WishlistRepo] (existing functionality).
 *
 * A username/email colliding with an existing user surfaces as
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] from
 * [adminFeature]'s `usersManagement.create`/`update` — caught here and translated to `409 Conflict`.
 */
```

`post(Constants.usersCreatePathPart)` handler — before:
```kotlin
                    post(Constants.usersCreatePathPart) {
                        requireAdmin() ?: return@post
                        val newUser = call.receive<NewUserWithPassword>()
                        val result = adminFeature.usersManagement.create(newUser)
                        if (result == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(result)
                        }
                    }
```

After:
```kotlin
                    post(Constants.usersCreatePathPart) {
                        requireAdmin() ?: return@post
                        val newUser = call.receive<NewUserWithPassword>()
                        val result = try {
                            adminFeature.usersManagement.create(newUser)
                        } catch (e: DuplicateUserFieldException) {
                            call.respond(HttpStatusCode.Conflict)
                            return@post
                        }
                        if (result == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(result)
                        }
                    }
```

`put("${Constants.usersUpdatePathPart}/{id}")` handler — before:
```kotlin
                    put("${Constants.usersUpdatePathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newUser = call.receive<NewUser>()
                        when (adminFeature.usersManagement.update(id, newUser)) {
                            true -> call.respond(HttpStatusCode.OK)
                            false -> call.respond(HttpStatusCode.InternalServerError)
                            null -> call.respond(HttpStatusCode.NotFound)
                        }
                    }
```

After:
```kotlin
                    put("${Constants.usersUpdatePathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newUser = call.receive<NewUser>()
                        val result = try {
                            adminFeature.usersManagement.update(id, newUser)
                        } catch (e: DuplicateUserFieldException) {
                            call.respond(HttpStatusCode.Conflict)
                            return@put
                        }
                        when (result) {
                            true -> call.respond(HttpStatusCode.OK)
                            false -> call.respond(HttpStatusCode.InternalServerError)
                            null -> call.respond(HttpStatusCode.NotFound)
                        }
                    }
```

No other handler in this file changes (`getAll`, `getById`, `setPassword`, `delete`, all wishlist/wishlist-item routes are unaffected — none writes `usernameColumn`/`emailColumn`).

**Minor style note for Coding (non-blocking):** all three `catch (e: DuplicateUserFieldException)` blocks leave `e` unused. This compiles fine (Kotlin warns, does not error — confirmed no `-Werror`/warnings-as-errors anywhere in `gradle/templates/defaultProject.gradle` or elsewhere), so it is not a spec change; Coding may use `catch (_: DuplicateUserFieldException)` instead if preferred, at its discretion.

---

## 5. README updates — exact text

### 5.1 `features/users/README.md`

**Architecture Notes — "Email field" bullet.** Before:
```
- **Email field (added in issue #44):** `User`, `NewUser`, and `RegisteredUser` all carry `email: Email? = null` (defaults to `null` for back-compat). `ExposedUsersRepo` has a `nullable text("email")` column; `createMissingTablesAndColumns` (via `initTable()`) adds it to existing tables without data migration. Invalid stored values are read defensively via `Email.parse(...).getOrNull()`. `features/users/common` now depends on `features/email/common` for the `Email` type.
```

After:
```
- **Email field (added in issue #44):** `User`, `NewUser`, and `RegisteredUser` all carry `email: Email? = null` (defaults to `null` for back-compat). `ExposedUsersRepo` has a `nullable text("email")` column, now also `.uniqueIndex()`-constrained — `NULL` values are exempt from the uniqueness check under standard SQL unique-index semantics, so users without a stored email never collide with each other, while two users sharing the same non-null email throws `DuplicateUserFieldException` (see "Duplicate-key-to-409 convention" below). `createMissingTablesAndColumns` (via `initTable()`) adds the column, and now also the unique index, to existing tables on first startup after this change, without a separate migration step. Invalid stored values are read defensively via `Email.parse(...).getOrNull()`. `features/users/common` now depends on `features/email/common` for the `Email` type.
```

**Models table — add row** (insert after the `ExposedUsersRepo` row):
```
| `DuplicateUserFieldException` | Thrown by `ExposedUsersRepo.update`/`create` on a Postgres unique-constraint violation (username or email); propagates through `CacheUsersRepo` untouched; callers should catch and respond `409 Conflict`. |
```

**Architecture Notes — new bullet**, appended after the existing "Self-service email update..." bullet:
```
- **Duplicate-key-to-409 convention:** `usernameColumn` and `emailColumn` are both `.uniqueIndex()`-constrained. `ExposedUsersRepo.update`/`create` override the library defaults (`AbstractExposedWriteCRUDRepo.update`/`create`) to catch `org.jetbrains.exposed.v1.exceptions.ExposedSQLException`, check `isUniqueViolation()` (SQL state `23505`, the Postgres `unique_violation` code), and translate a match into `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException` — every other exception is rethrown unchanged. `CacheUsersRepo`'s `FullCRUDCacheRepo` write wrapper does not catch exceptions from the wrapped repo, so the exception propagates through it untouched. Consumers that need to distinguish "duplicate" from other failures catch `DuplicateUserFieldException` at the HTTP boundary and respond `409 Conflict` instead of the generic `500 Internal Server Error` an unmapped exception would otherwise produce (Ktor's engine-level `DefaultEnginePipeline.handleFailure` fallback). Current consumers: `features/email/server`'s `PUT /email/myEmail`, and `features/admin/server`'s `PUT /admin/users/update/{id}` and `POST /admin/users/create`. A future feature hitting the same shape (a unique-constrained write whose caller needs a distinguishable "duplicate" signal) should follow this same pattern rather than invent a new one.
```

### 5.2 `features/email/README.md`

**Routes table — `PUT /email/myEmail` row.** Before:
```
| PUT | `/email/myEmail` | Bearer (self) | `SetEmailRequest { email: Email? }` / `200 OK` | Stores or clears the authenticated caller's own email address |
```

After:
```
| PUT | `/email/myEmail` | Bearer (self) | `SetEmailRequest { email: Email? }` / `200 OK` or `409 Conflict` | Stores or clears the authenticated caller's own email address; `409` when the address is already stored for a different user |
```

**Architecture Notes — new bullet**, inserted immediately after the existing "Root guard" bullet:
```
- **Duplicate email → 409:** `PUT /email/myEmail`'s handler wraps only the `feature.setMyEmail(...)` call in a `try`/`catch (e: DuplicateUserFieldException)`, responding `409 Conflict` and returning before the `when { updated -> ... }` block runs. The exception originates in `ExposedUsersRepo.update` (see `features/users/README.md`'s "Duplicate-key-to-409 convention") and propagates unchanged through `updateStoredEmail`/`EmailFeatureService.setMyEmail`/`DisabledEmailFeature.setMyEmail` — none of those three add a `try`/`catch` of their own, by design; only the HTTP boundary (`EmailRoutingsConfigurator`) does. `sendTest`'s handler is unaffected — it writes no unique-constrained column.
```

### 5.3 `features/admin/README.md`

**Routes table — `Users Management` rows.** Before:
```
| POST | `/admin/users/create` | `NewUserWithPassword` | `RegisteredUser` | Create user with plaintext password |
| PUT | `/admin/users/update/{id}` | `NewUser` | `200 OK` / `404` | Update user info by id |
```

After:
```
| POST | `/admin/users/create` | `NewUserWithPassword` | `RegisteredUser` / `500` / `409` | Create user with plaintext password; `409` when the username is already taken |
| PUT | `/admin/users/update/{id}` | `NewUser` | `200 OK` / `404` / `409` | Update user info by id; `409` when the new username or email is already taken |
```

**Architecture Notes ("Server side" subsection) — new bullet**, inserted immediately after the existing `AdminRoutingsConfigurator` bullet:
```
- **Duplicate username/email → 409:** `AdminRoutingsConfigurator`'s `POST /admin/users/create` and `PUT /admin/users/update/{id}` handlers each wrap only the `adminFeature.usersManagement.create(...)`/`update(...)` call in a `try`/`catch (e: DuplicateUserFieldException)`, responding `409 Conflict` and returning before the existing `if (result == null)`/`when (result)` branch runs. The exception originates in `ExposedUsersRepo.update`/`create` (see `features/users/README.md`'s "Duplicate-key-to-409 convention") and propagates unchanged through `UsersManagementFeature.create`/`update` — neither adds a `try`/`catch` of its own; only the HTTP boundary (`AdminRoutingsConfigurator`) does. No other admin route is affected.
```

---

## 6. Final file list for Coding

**New files (3):**
1. `features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt` — §1.1
2. `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` — §2.1
3. *(no third new source file — `FakeUsersRepo.kt` below is a modification of an existing file)*

**Modified source files (8):**
1. `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` — §1.2 (mandatory: `.uniqueIndex()`, `update`/`create` overrides, `isUniqueViolation`)
2. `features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt` — §1.3 (KDoc only)
3. `features/users/common/src/commonMain/kotlin/repo/CacheUsersRepo.kt` — §1.4 (KDoc only)
4. `features/email/server/src/commonMain/kotlin/EmailFeature.kt` — §1.5 (KDoc only)
5. `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — §1.6 (KDoc only)
6. `features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt` — §1.7 (KDoc only)
7. `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt` — §1.8 (KDoc only)
8. `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — §1.9 (KDoc only)

**Modified route configurators (2):**
9. `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` — §4.1
10. `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — §4.2

**Modified test files (3):**
11. `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` — §2.2
12. `features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt` — §2.3 (add one `@Test`, two new imports)
13. `features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt` — §2.4 (add one `@Test`, two new imports)

**Modified READMEs (3):**
14. `features/users/README.md` — §5.1
15. `features/email/README.md` — §5.2
16. `features/admin/README.md` — §5.3

**Build/verification, per `agents/CODING.md`'s post-change build-task rule (one cycle of fixing on failure; do not re-fix the same issue twice — report instead):**
```
./gradlew :wishlist.features.users.common:build
./gradlew :wishlist.features.email.server:build
./gradlew :wishlist.features.admin.server:build
```
plus the full-project build task. Run `ast-index rebuild` after the `.kt` edits (README-only edits do not require it, per `agents/ALL.md`).

No `build.gradle` changes anywhere — verified independently in this pass (not just trusted from round 2): `features/email/server` already depends on `features/users/common` (transitively via `api project(":wishlist.features.users.common")`, confirmed by its existing unqualified imports of `UsersRepo`/`NewUser`/etc.); `features/admin/server` already imports `ReadUsersRepo`/`NewUser`/`UserId` from the same module today; `jvmTest`/`commonTest` are wired for `features/users/common` by the existing `enableMPPJvm.gradle`/`defaultProject.gradle` templates with no per-module opt-in needed.

---

## 7. Flags for the operator / Coding

- **No blocking ambiguity remains.** `002-planning.md` was already READY and fully investigated; this pass found zero drift between its assumed "before" states and the actual current source, and independently re-verified (against real library sources, not just citation) every non-obvious technical claim the design depends on (§0). Nothing here contradicts or revises `002-planning.md`'s design.
- **Two explicit calls made by this step** (both requested by this task's brief, neither left undecided): (a) §3 — no Ktor route-test harness, no shared duplicate→409 helper; the four-layer unit/service coverage in §2 is the accepted, intentional stopping point. (b) §1.3/§1.4/§1.9 — promoted `002-planning.md` §4.6's "optional" KDoc additions (`WriteUsersRepo`, `CacheUsersRepo`, `UsersManagementFeature`) to mandatory, since this task's own brief named them explicitly and `agents/CODING.md`'s KDoc rule reads as a blanket requirement, not a new-files-only one.
- **Untestable-by-design items are named explicitly in §3**, per `agents/ARCHITECTURE.md`'s requirement to flag rather than silently skip: route-level HTTP 409 assertions, `admin/server`-level unit tests (no test harness exists there at all), and the real-Postgres constraint-violation path. None of these block "READY" — they are pre-existing, repo-wide testing-infrastructure gaps this task does not need to close, consistent with this repo's own documented precedent (`features/email/README.md`'s "live-SMTP path intentionally not unit-tested").
- **One pre-existing, non-blocking risk already flagged by round 1 (§2.5), repeated here for visibility going into Coding:** if any already-migrated dev/test database currently holds two or more users with the same non-null email, `initTable()`'s `CREATE UNIQUE INDEX` will fail loudly at the next server startup (not per-request). Given the whole email feature is still on this unmerged branch, this is expected to be a non-issue in practice, but Coding/Verification should be aware a local dev DB with stale duplicate test data could surface it.

**Status: READY for Coding.** Path to this file: `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/003-architecturing.md`.
