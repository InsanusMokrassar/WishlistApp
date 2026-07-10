Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~40 minutes wall-clock
Tokens used: not precisely instrumented from inside this agent; rough estimate ~110k-140k tokens (repo file reads, `ast-index` setup checks, three additional decompiled-library-sources jar extractions/reads beyond the ones already extracted in round 1: `micro_utils.repos.cache` 0.29.4, `micro_utils.repos.exposed-jvm` 0.29.4 full `AbstractExposedWriteCRUDRepo`/`AbstractExposedCRUDRepo`, `exposed-core`/`exposed-jdbc` 1.3.0 `Exceptions.kt`/`BlockingExecutable.kt`).
Changed files: `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/002-planning.md` (this file) only. No source, config, or other doc file was edited by this step.

---

# Planning round 2: duplicate-key-to-409 convention (email + username), per operator's option-(C) answer

## 1. Task understanding

Round 1 (`001-planning.md`) resolved the literal review comment (`.uniqueIndex()` on `emailColumn`) and flagged one open question: whether to *also* add a distinguishable `409 Conflict` signal for "duplicate key" beyond the mandatory fix, since the codebase had zero existing precedent either way (not even for `usernameColumn`, which has had the identical unguarded gap for longer). Three options were laid out (A: do nothing extra, B: fix email only, C: fix both email and the pre-existing username gap as a general convention).

**Operator's answer: option (C).** Establish a *general* duplicate-key-to-409 convention, not a one-off for `email`, and close the pre-existing `username` gap in the same pass (`admin/server/UsersManagementFeature.update` → `AdminRoutingsConfigurator`'s `PUT /admin/users/update/{id}`, per round 1 §2.4's finding).

This round's job: design the concrete mechanism (exception type, package, throw site, catch sites) and produce exact diffs for every affected file, verifying every non-obvious claim (does `CacheUsersRepo` need changes? is `AbstractExposedWriteCRUDRepo.update`/`create` actually overridable?) from real library sources rather than assuming.

## 2. Investigation

### 2.1 Where does the exception actually originate — and where can `ExposedUsersRepo` actually catch it?

Round 1 established that a unique-constraint violation surfaces as an uncaught `ExposedSQLException` (`org.jetbrains.exposed.v1.exceptions.ExposedSQLException`, extends `java.sql.SQLException`) from deep inside the library. This round I verified the **exact call shape** by reading the full `AbstractExposedWriteCRUDRepo.kt`/`AbstractExposedCRUDRepo.kt` (`micro_utils.repos.exposed-jvm-0.29.4-sources.jar`):

- `ExposedUsersRepo` currently only overrides the **abstract fill-in** `update(id: IdType?, value: InputValueType, it: UpdateBuilder<Int>)` — this just sets column values on an `UpdateBuilder`/`InsertStatement`; it runs *before* any SQL is executed against the DB, so it can **never** see a constraint-violation exception. Catching here would be a no-op.
- The actual DB write (and thus the actual `ExposedSQLException` throw site) happens inside the library's own `updateWithoutNotification`/`createWithoutNotification` private functions, called from the library's `public override suspend fun update(id: IdType, value: InputValueType): ObjectType?` and `public override suspend fun create(values: List<InputValueType>): List<ObjectType>` — **neither of these is currently overridden by `ExposedUsersRepo`**.
- Critically, in Kotlin, a member declared `override` (without `final`) is itself open by default. Both `update(id, value)` and `create(values)` in `AbstractExposedWriteCRUDRepo` are plain `override suspend fun` (no `final`), and `AbstractExposedCRUDRepo` re-declares neither — so **`ExposedUsersRepo` can override `update(id: UserId, value: NewUser): RegisteredUser?` and `create(values: List<NewUser>): List<RegisteredUser>` directly**, wrap `super.update(...)`/`super.create(...)` in a `try`/`catch`, and this is exactly where the real DB exception is visible. This is the correct, and only correct, catch site inside `ExposedUsersRepo`.

### 2.2 `ExposedSQLException`'s SQL-state surface (exposed-core 1.3.0, `Exceptions.kt`)

```kotlin
class ExposedSQLException(
    cause: Throwable?,
    val contexts: List<StatementContext>,
    private val transaction: Transaction
) : SQLException(cause) {
    private val originalSQLException = cause as? SQLException
    override fun getSQLState(): String = originalSQLException?.sqlState.orEmpty()
    ...
}
```

`getSQLState()` delegates to the wrapped driver exception's `sqlState`, exposed via the inherited `java.sql.SQLException.sqlState` property. Postgres reports `23505` for `unique_violation` (standard SQLSTATE code, not Postgres-driver-specific — `java.sql.SQLException.sqlState` is part of the JDBC spec). So `exposedSqlException.sqlState == "23505"` is the correct, vendor-neutral-at-the-JDBC-level check. `exposed-core`/`exposed-jdbc` are already transitive dependencies of `features/users/common`'s jvmMain (already imported for `Op`, `ResultRow`, etc.), so **no new `build.gradle` dependency is needed** to reference `ExposedSQLException`.

### 2.3 Does `CacheUsersRepo` need any change? (verified from `micro_utils.repos.cache-0.29.4-sources.jar`)

`CacheUsersRepo` is `UsersRepo, FullCRUDCacheRepo<...>(crudRepo = originalRepo, ...)`. `FullCRUDCacheRepo`'s write behavior is delegated (`by WriteCRUDCacheRepo(crudRepo, ...)`, from `dev.inmo.micro_utils.repos.cache.CRUDCacheRepo.kt`):

```kotlin
override suspend fun update(id: IdType, value: InputValueType): ObjectType? {
    return parentRepo.update(id, value) ?.also {
        locker.withWriteLock { kvCache.unset(id); kvCache.set(idGetter(it), it) }
    }
}
override suspend fun create(values: List<InputValueType>): List<ObjectType> {
    val created = parentRepo.create(values)
    locker.withWriteLock { kvCache.set(created.associateBy { idGetter(it) }) }
    return created
}
```

Neither has a `try`/`catch`. `parentRepo.update(id, value)` is called unguarded; `?.also { }` only runs on a non-null *result* — it does not intercept exceptions. An exception thrown by the wrapped repo (`ExposedUsersRepo`) propagates straight through `CacheUsersRepo` untouched, both for `update` and `create`. **Confirmed: `CacheUsersRepo.kt` needs zero code changes.** (It also currently has no class-level KDoc at all — see §5.3 for an optional, non-blocking doc addition.)

Also confirmed, re-reading `CacheUsersRepo.kt`: it lives in `features/users/common/src/**commonMain**/kotlin/repo/` — not `jvmMain` as the round-1 handoff prompt assumed. This matches the CRUD Repository Pattern doc in `agents/CODING.md` exactly (`CacheItemsRepo` is documented as a `commonMain` type, generic over any `ItemsRepo`, not Exposed-specific) — it is platform-agnostic by design, which is exactly why it needs no Postgres-specific knowledge and no change here.

### 2.4 Client-side impact (verified by reading `KtorEmailFeature.kt` and `KtorUsersManagementFeature.kt`)

Both `KtorEmailFeature.setMyEmail` and `KtorUsersManagementFeature.update`/`create` gate success purely on `response.status.isSuccess()` (true only for 2xx). A `409` is therefore treated identically to today's `500` from the client's perspective — `false`/`null`, no crash, no behavior change, no client code needs to change for correctness. Distinguishing "duplicate" in the UI (e.g. a specific error message) would be a separate, future UX task — **explicitly out of scope for this round**, not silently dropped.

### 2.5 No existing exception-class or `exceptions` package precedent anywhere in the repo

Repo-wide grep for `class.*Exception` under `features/` (excluding `build/`) returns zero hits. This is genuinely new precedent, as round 1 already found for the 409-response side. The design below is intentionally the smallest concrete pattern that satisfies "general convention" without speculatively building a cross-feature abstraction no other feature needs yet (YAGNI — if a future feature repo needs the same shape, it can define its own analogous exception next to its own repo, following this one as the reference).

## 3. Design decision

### 3.1 Exception type: `DuplicateUserFieldException`

**Location:** `features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt`
**Package:** `dev.inmo.wishlist.features.users.common.repo.exceptions`

Rationale for `commonMain` (not `jvmMain`, even though only the JVM `ExposedUsersRepo` ever throws it): the type must be referenceable from `features/email/server` and `features/admin/server`'s route handlers, which are `commonMain`-compiled Ktor route code (JVM-executed, but written in `commonMain` source sets per this repo's `server` module shape — see `EmailRoutingsConfigurator.kt`/`AdminRoutingsConfigurator.kt`, both under `src/commonMain/kotlin/configurators/`). A `commonMain` exception type, thrown only by the `jvmMain` implementation, propagating through interface calls, is the standard KMP shape for this — Kotlin has no checked exceptions, so there's no compile-time contract to satisfy, only a documented one (via KDoc `@throws`, applied below).

Rationale for **one type, not per-field subtypes** (`DuplicateUsernameException`/`DuplicateEmailException`): every catch site in this plan only needs "was this a duplicate-key violation → respond 409", never "which specific field" — HTTP responses stay a bare `409 Conflict` with no body distinguishing which field collided (matching this repo's existing precedent of coarse-grained status-only error signaling, e.g. `sendTest`'s already-documented "root vs SMTP failure indistinguishable at the HTTP layer" pattern). Extracting the Postgres constraint name for field-level detail would require depending on `org.postgresql.util.PSQLException` (a JDBC-driver-specific type) inside `features/users/common`, which is unnecessary coupling for no consumer that needs it today.

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

### 3.2 SQL-state check as a standalone, testable function

Added at the bottom of `ExposedUsersRepo.kt` (file scope, outside the class) rather than inlined in the `catch` block, specifically so it is unit-testable against a plain `java.sql.SQLException` without a live database:

```kotlin
/**
 * Returns whether this exception represents a Postgres `unique_violation` (SQL state `23505`) —
 * i.e. a `.uniqueIndex()`-constrained column already holds the given value for a different row.
 *
 * A standalone, pure function (rather than inlined in [ExposedUsersRepo]'s catch blocks) so it
 * can be unit-tested directly against a plain [SQLException], without a live database.
 */
internal fun SQLException.isUniqueViolation(): Boolean = sqlState == "23505"
```

### 3.3 Catch points: each route handler, tight-scoped around only the repo-touching call

Per the task's own framing (and confirmed by there being no shared error-translation helper anywhere in this repo — `StatusPages` is installed with zero registered handlers, per round 1 §2.3), the catch happens **in each route handler**, wrapped tightly around only the call that can throw (not the whole handler body, so `call.receive<...>()` deserialization failures and other unrelated exceptions are not accidentally swallowed as 409). No shared "duplicate → 409" Ktor helper is introduced — with exactly three call sites (`PUT /email/myEmail`, `PUT /admin/users/update/{id}`, `POST /admin/users/create`, see §3.4), a three-line `try { ... } catch (e: DuplicateUserFieldException) { call.respond(HttpStatusCode.Conflict); return@X }` at each site is simpler and more obviously correct than a shared abstraction with only three (non-uniform: `put`/`put`/`post`, different return types `Boolean`/`Boolean?`/`RegisteredUser?`) call sites.

### 3.4 Scope decision: also fix `POST /admin/users/create` (username collision), not just the literally-named `update` route

Round 1 §2.4's finding, and the operator's answer, name `UsersManagementFeature.update`/`PUT /admin/users/update/{id}` specifically. `UsersManagementFeature.create` (→ `POST /admin/users/create`) writes the same `usernameColumn` via the same `usersRepo.create(...)` path (confirmed: `AdminRoutingsConfigurator`'s create handler already responds `500` on `null`, exactly mirroring `update`'s pre-existing gap) and — once `ExposedUsersRepo.create` is overridden per §3.1 — would **also** throw `DuplicateUserFieldException` on a duplicate username, landing on the same generic `500` as `update` did before this fix, if left uncaught. Leaving `create` unguarded while fixing `update` would produce exactly the inconsistent, half-general "one-off hack" outcome the operator explicitly said they want to avoid. This is a scope-breadth call with **no design ambiguity** (the mechanism and diff shape are identical to `update`'s), so per `agents/PLAN.md` step 6 ("plan the concrete changes") I am including it directly rather than raising it as a new open question — flagged here explicitly so it's auditable if the operator wants to veto it before Coding.

No other admin route needs this treatment: `PUT /admin/users/setPassword/{id}` delegates to `AuthFeatureService.setPassword` (no unique-constrained column involved), and none of the wishlist/wishlist-item routes touch `usernameColumn`/`emailColumn`.

## 4. Exact diffs

### 4.1 `features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt` — **new file**

Full content given in §3.1.

### 4.2 `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` — mandatory

Full new content (comments show what's new vs. round 1's already-decided `.uniqueIndex()` diff):

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

### 4.3 `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` — mandatory

New import: `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

Class KDoc bullet update:
```kotlin
 * - `PUT  /email/myEmail`   — bearer (self-service); the caller identity is passed to [feature]
 *   which persists the address, responding `409 Conflict` when the address is already stored for
 *   a different user
 *   (see [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException]).
```

`put(myEmailPathPart)` handler — before/after:

```kotlin
// before
put(EmailConstants.myEmailPathPart) {
    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
    val request = call.receive<SetEmailRequest>()
    val updated = feature.setMyEmail(callerId, request.email)
    when {
        updated -> call.respond(HttpStatusCode.OK)
        else -> call.respond(HttpStatusCode.InternalServerError)
    }
}

// after
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

`sendTest` handler is unchanged (root-only check + SMTP send, no unique-constrained write).

### 4.4 `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — mandatory (both `update` and `create`, per §3.4)

New import: `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

Class KDoc bullet updates:
```kotlin
 * - `POST   /admin/users/create`         — create user with password; body: [NewUserWithPassword]; `409` on duplicate username
 * - `PUT    /admin/users/update/{id}`    — update user info; body: [NewUser]; `409` on duplicate username/email
```

`post(usersCreatePathPart)` handler — before/after:

```kotlin
// before
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

// after
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

`put("${usersUpdatePathPart}/{id}")` handler — before/after:

```kotlin
// before
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

// after
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

No other handler in this file changes (`setPassword`, `delete`, all wishlist/wishlist-item routes are unaffected — see §3.4).

No `build.gradle` changes anywhere: `features/email/server` already has `api project(":wishlist.features.users.common")`; `features/admin/server` already transitively resolves `features/users/common` types (it already imports `ReadUsersRepo`, `NewUser`, `UserId` from that module today, confirmed by reading the current file) — the new exception type lives in the same module, so it is already on the classpath.

### 4.5 KDoc-only updates — **mandatory** (files that already carry KDocs whose documented contract is changing; per `agents/CODING.md`: "When updating existing code that has KDocs — update the KDocs to match")

- **`features/email/server/src/commonMain/kotlin/EmailFeature.kt`** — add to `setMyEmail`'s KDoc:
  ```kotlin
  * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
  *   when [email] is already stored for a different user.
  ```
- **`features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`** — same `@throws` line added to `setMyEmail`'s KDoc, with "propagates unchanged from `updateStoredEmail`/`UsersRepo.update`" appended.
- **`features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt`** — same `@throws` line added to `setMyEmail`'s KDoc.
- **`features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt`** — same `@throws` line added to `updateStoredEmail`'s KDoc, noting: "this function does not catch it, by design — `EmailRoutingsConfigurator` catches it at the HTTP boundary and responds `409 Conflict`."

None of these four files need any **code** change — the exception already propagates through their existing bodies untouched (they have no `try`/`catch` today and none is being added; only `ExposedUsersRepo` and the two route-handler files get code changes).

### 4.6 KDoc-only updates — **optional/recommended, not blocking** (declarations with no pre-existing KDoc block, so `agents/CODING.md`'s "update existing KDocs" rule does not strictly apply, but documenting a new exception-throwing contract at the interface/service boundary is good practice)

- **`features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt`** — add a class KDoc pointing at `DuplicateUserFieldException`'s own KDoc for the full propagation path.
- **`features/users/common/src/commonMain/kotlin/repo/CacheUsersRepo.kt`** — add a class KDoc noting the pass-through confirmed in §2.3 (currently has zero class-level KDoc).
- **`features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt`** — add `@throws DuplicateUserFieldException` KDoc to `update` and `create` (neither currently has any KDoc block, unlike `setPassword`/`delete` in the same file, which do).

Architecture/Coding may include or skip §4.6 at their discretion — they do not gate "done."

## 5. Test coverage

### 5.1 New, genuinely automatable unit test: `isUniqueViolation`

`features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` — **new file** (this is `features/users/common`'s first-ever test file; the `mppJvmJsAndroid` template's underlying `defaultProject` already wires `kotlin.test` for every module, matching how `features/email/server`'s existing tests need no extra `build.gradle` entry):

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

This is the one piece of new logic in `ExposedUsersRepo.kt` that *can* be tested without a live Postgres instance — consistent with this repo's existing precedent (`emailConfigElementOrNull` in `email/server/Plugin.kt`, extracted specifically to be unit-testable). The `try`/`catch` wiring around `super.update`/`super.create`, and the DB-level uniqueness enforcement itself, remain **not** unit-tested, matching the repo-wide precedent already established for DB-backed behavior (round 1 §... / `features/email/README.md`'s "live-SMTP success path intentionally not unit-tested" note) — verified instead via the module build plus (optionally) a manual `PUT /email/myEmail` / `PUT|POST /admin/users/...` duplicate check against a real Postgres instance.

### 5.2 `FakeUsersRepo` — enforce uniqueness so the propagation contract is testable at the service layer

`features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` currently has no uniqueness enforcement at all (plain `MapCRUDRepo` wrapper). Make it a faithful test double by enforcing the same collision rule `ExposedUsersRepo` enforces (unique username always; unique non-null email; a record never collides with itself):

```kotlin
override suspend fun updateObject(newValue: NewUser, id: UserId, old: RegisteredUser): RegisteredUser {
    if (getAll().values.any { it.id != id && it.username == newValue.username }) {
        throw DuplicateUserFieldException()
    }
    if (newValue.email != null && getAll().values.any { it.id != id && it.email == newValue.email }) {
        throw DuplicateUserFieldException()
    }
    return old.copy(username = newValue.username, email = newValue.email)
}

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
```

New import: `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`. Update the class KDoc to mention the new uniqueness enforcement. **Verified this does not break any existing test**: every current fixture in `EmailFeatureServiceTest`/`DisabledEmailFeatureTest` uses distinct usernames (`"root"` vs `"alice"`) and non-colliding emails across every seeded map — read both files in full to confirm no existing scenario relies on two stored users sharing a username/email or on updating a user's own already-stored value (which correctly stays a no-op collision, guarded by the `it.id != id` check).

### 5.3 New propagation tests (service layer — this repo's existing test depth for this feature)

- **`features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt`** — add:
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
  New imports: `kotlin.test.assertFailsWith`, `dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException`.

- **`features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt`** — add the analogous test against `DisabledEmailFeature` (same fixtures, same assertion shape), confirming the SMTP-disabled path propagates the exception identically to the SMTP-enabled path (mirrors this file's existing "storage stays independent of SMTP" theme).

These give real, executable regression coverage of "duplicate collision → exception propagates unchanged through the service layer" — the layer this repo's existing tests already reach for this feature — without requiring a live database or a new Ktor test harness.

### 5.4 Explicitly flagged as not automatable this round (per `agents/ARCHITECTURE.md`'s Test Planning Requirement — routed to Architecture, not silently skipped)

- **Route-level HTTP assertions** (`EmailRoutingsConfigurator`/`AdminRoutingsConfigurator` actually returning `409`): repo-wide check confirms **zero** `testApplication`/`ApplicationTestBuilder` usage anywhere in this codebase today — there is no established Ktor route-test harness to extend, for any feature, not just this one. Introducing one is a bigger, cross-cutting infrastructure decision outside this task's scope.
- **`UsersManagementFeature`-level unit tests** (admin/server): `features/admin/server` has **zero** test files today (confirmed by directory search). Testing `update`/`create`'s new exception-propagation behavior properly would require standing up this module's first-ever test harness, including fakes for `AuthFeatureService`, `WishlistRepo`, `WishlistItemRepo` — a larger lift than this task's scope; the equivalent coverage this round is delivered one layer down, in `ExposedUsersRepo`'s `isUniqueViolation` unit test (§5.1) and in the email-side service-layer propagation tests (§5.3), which exercise the identical propagation contract from `UsersRepo.update`/`create` upward.
- **`ExposedUsersRepo.update`/`create`'s actual `try`/`catch` wiring against a real Postgres constraint violation**: not unit-testable without a live database, matching this repo's existing, already-documented precedent for DB-backed behavior (no exception here).

None of these three gaps block "READY" — they are pre-existing repository-wide testing-infrastructure gaps (no Ktor test harness, no `admin/server` tests at all) that this task does not need to close to deliver the operator's requested behavior change, but Architecture should carry them forward per the Test Planning Requirement rather than assume they were overlooked.

## 6. README updates (mandatory — `agents/CODING.md`/`agents/ARCHITECTURE.md` "after any code change" rules)

- **`features/users/README.md`**:
  - Architecture Notes, "Email field (added in issue #44)" bullet: add the `.uniqueIndex()` clause (already decided in round 1, now finalized with the exact wording matching §4.2's column KDoc).
  - Models table: add a row for `DuplicateUserFieldException` (`Thrown by ExposedUsersRepo.update/create on a Postgres unique-constraint violation (username or email); propagates through CacheUsersRepo untouched; callers should catch and respond 409 Conflict.`).
  - New Architecture Notes bullet: **"Duplicate-key-to-409 convention"** — documents the mechanism end to end (`ExposedUsersRepo` catches `ExposedSQLException`/SQL state `23505` → `DuplicateUserFieldException` → caught at each HTTP route boundary), and names both consumers (`features/email/server`'s `PUT /email/myEmail`, `features/admin/server`'s `PUT /admin/users/update/{id}` and `POST /admin/users/create`) so a future feature repo hitting the same shape has a documented reference to follow.
- **`features/email/README.md`**:
  - Routes table: `PUT /email/myEmail` row's response column changes from `200 OK` to `200 OK` / `409 Conflict`.
  - Architecture Notes: new bullet next to the existing "Root guard" note, documenting the new `409` case and linking it to `DuplicateUserFieldException`.
- **`features/admin/README.md`**:
  - Routes table: `PUT /admin/users/update/{id}` row's response column changes from `200 OK / 404` to `200 OK / 404 / 409`; `POST /admin/users/create` row's response column (`RegisteredUser`) gains `/ 409` alongside its existing implicit `500` (not currently spelled out in that row either — worth tightening while touching it).
  - Architecture Notes ("Server side" subsection): new bullet documenting `AdminRoutingsConfigurator`'s new `DuplicateUserFieldException` catch on both the create and update user routes.

## 7. Build/verification

Per `agents/CODING.md`'s post-change build-task rule, run (one cycle of fixing on failure, do not re-fix the same issue twice — report instead):
- `./gradlew :wishlist.features.users.common:build`
- `./gradlew :wishlist.features.email.server:build`
- `./gradlew :wishlist.features.admin.server:build`
- The full-project build task.

`ast-index rebuild` after the `.kt` edits (README-only edits do not require it, per `agents/ALL.md`).

## 8. Answers received / open questions status

**Operator's answer to round 1's §4 question: option (C)**, recorded verbatim in this task's orchestrator-provided brief (see top of this file's originating prompt) — fully incorporated into §3-§6 above.

No new blocking ambiguity surfaced while broadening scope to the concrete mechanism design. The two judgment calls made this round (§3.4's inclusion of `POST /admin/users/create`, and §4.6's "optional" KDoc additions) both have zero technical ambiguity in *how* to execute them — only a scope-breadth choice, made in the direction the operator's own "general convention, not one-off" framing already points, and both are called out explicitly above so they're auditable rather than silently smuggled in.

**READY for Architecture.**
