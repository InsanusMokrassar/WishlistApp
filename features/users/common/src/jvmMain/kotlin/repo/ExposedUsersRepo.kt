package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.UpdatedValuePair
import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.common.common.utils.isUniqueViolation
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.booleanLiteral
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update as exposedUpdate

/**
 * Singleton database-owned mutex for all participating users writers.
 *
 * The row with [idColumn] equal to one is updated as the first database operation in every users
 * write transaction. PostgreSQL row locking and SQLite's single-writer reservation then serialize
 * raw cross-slot uniqueness checks and the matching mutation across repository instances.
 */
private object UsersWriteLockTable : Table("users_write_lock") {
    /** Stable singleton row identifier. */
    val idColumn = integer("id")

    /** Value rewritten by a lock acquisition without changing persistent state. */
    val markerColumn = integer("marker")

    override val primaryKey = PrimaryKey(idColumn)
}

/**
 * Exposed-backed PostgreSQL and SQLite implementation of [UsersRepo].
 *
 * Stores users in the `users` table with an auto-increment `id`, a unique `username`, and a
 * unique, nullable `email` column. The nullable `email` column is additive — `initTable()` adds
 * it to any existing table via `createMissingTablesAndColumns`, keeping existing rows valid with
 * `NULL`; `NULL` values are exempt from the uniqueness check, so users without a stored email
 * never collide with each other.
 *
 * Every write first updates the singleton [UsersWriteLockTable] row, then performs raw current and
 * pending-address checks before changing data. Write notifications are emitted only after that
 * transaction commits. Other constraint and database failures retain their original type.
 *
 * @param database Exposed [Database] instance (provided by the common server plugin).
 */
class ExposedUsersRepo(
    override val database: Database,
    private val nowMillis: () -> Long = System::currentTimeMillis,
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

    /**
     * Durable approval evidence for [emailColumn].
     *
     * The non-null default makes the additive migration conservative: every historic row starts
     * pending instead of inferring mailbox ownership from a role or a previously stored address.
     */
    private val emailApprovedColumn = bool("email_approved").default(false)

    /** Replacement candidate retained while the current address remains approved. */
    private val pendingEmailColumn = text("pending_email").nullable().index()

    /** Nullable UTC epoch-millisecond deadline issued after a new approval. */
    private val emailChangeAllowedAtColumn = long("email_change_allowed_at").nullable()

    override val primaryKey = PrimaryKey(idColumn)

    /**
     * Maps a result row to a [RegisteredUser].
     *
     * Uses [Email.parse] defensively to avoid throwing on any legacy or manually-inserted
     * malformed rows — invalid stored values are treated as absent (`null`).
     */
    override val ResultRow.asObject: RegisteredUser
        get() {
            val email = get(emailColumn)?.let { Email.parse(it).getOrNull() }
            return RegisteredUser(
                id = UserId(get(idColumn)),
                username = Username(get(usernameColumn)),
                email = email,
                emailApproved = email != null && get(emailApprovedColumn),
                pendingEmail = get(pendingEmailColumn)?.let { Email.parse(it).getOrNull() },
                emailChangeAllowedAt = get(emailChangeAllowedAtColumn),
            )
        }

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
        if (id == null || value.email == null) {
            it[emailApprovedColumn] = false
        } else {
            it[emailApprovedColumn] = case()
                .When(emailColumn eq value.email.string, emailApprovedColumn)
                .Else(booleanLiteral(false))
        }
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
            email = value.email,
            emailApproved = false
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

    override suspend fun setEmail(id: UserId, email: Email?): RegisteredUser? =
        mutateEmail(id = id, email = email, username = null)

    override suspend fun updateUsername(id: UserId, username: Username): RegisteredUser? {
        val updated = try {
            transaction(db = database) {
                acquireWriteLock()
                val current = selectUser(id) ?: return@transaction null
                this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) { it[usernameColumn] = username.string }
                selectUser(id) ?: current
            }
        } catch (error: ExposedSQLException) {
            if (error.isUniqueViolation()) throw DuplicateUserFieldException(cause = error) else throw error
        }
        updated?.let { _updatedObjectsFlow.emit(it) }
        return updated
    }

    /**
     * Persists [value] over the row identified by [id].
     *
     * Wraps the library default (`AbstractExposedWriteCRUDRepo.update`) with translation of an
     * exact PostgreSQL or SQLite unique-violation on [usernameColumn]/[emailColumn] into
     * [DuplicateUserFieldException]. Every other exception and the plain `null` return for
     * "no such id" are unchanged.
     *
     * @param id Target user id.
     * @param value Replacement user data.
     * @return Updated [RegisteredUser], or `null` when [id] does not exist.
     * @throws DuplicateUserFieldException when [value]'s username or email is already used by a
     *   different user.
     */
    override suspend fun update(id: UserId, value: NewUser): RegisteredUser? =
        mutateEmail(id = id, email = value.email, username = value.username)

    /**
     * Inserts [values] as new users.
     *
     * Wraps the library default (`AbstractExposedWriteCRUDRepo.create`) with translation of an
     * exact PostgreSQL or SQLite unique-violation on [usernameColumn]/[emailColumn] into
     * [DuplicateUserFieldException].
     *
     * @param values New users to insert.
     * @return Inserted [RegisteredUser]s with generated ids.
     * @throws DuplicateUserFieldException when any of [values]' usernames or emails collides with
     *   an existing user.
     */
    override suspend fun create(values: List<NewUser>): List<RegisteredUser> {
        onBeforeCreate(values)
        val created = try {
            transaction(db = database) {
                acquireWriteLock()
                values.map { value ->
                    ensureEmailAvailable(email = value.email, excludedId = null)
                    insert { statement -> update(id = null, value = value, it = statement) }.asObject(value)
                }
            }
        } catch (error: ExposedSQLException) {
            if (error.isUniqueViolation()) throw DuplicateUserFieldException(cause = error) else throw error
        }
        val result = onAfterCreate(values.zip(created))
        result.forEach { _newObjectsFlow.emit(it) }
        return result
    }

    /** Executes the inherited bulk-update contract in one locked transaction and publishes after commit. */
    override suspend fun update(values: List<UpdatedValuePair<UserId, NewUser>>): List<RegisteredUser> {
        onBeforeUpdate(values)
        val updated = try {
            transaction(db = database) {
                acquireWriteLock()
                values.mapNotNull { (id, value) ->
                    mutateEmailInTransaction(id = id, email = value.email, username = value.username)
                        ?.let { value to it }
                }
            }
        } catch (error: ExposedSQLException) {
            if (error.isUniqueViolation()) throw DuplicateUserFieldException(cause = error) else throw error
        }
        val result = onAfterUpdate(updated)
        result.forEach { _updatedObjectsFlow.emit(it) }
        return result
    }

    /** Deletes existing ids under the singleton lock and publishes only after commit. */
    override suspend fun deleteById(ids: List<UserId>) {
        onBeforeDelete(ids)
        val deleted = transaction(db = database) {
            acquireWriteLock()
            val count = deleteWhere { selectByIds(ids) }
            if (count == ids.size) {
                ids
            } else {
                ids.filter { id -> selectUser(id) == null }
            }
        }
        deleted.forEach { _deletedObjectsIdsFlow.emit(it) }
    }

    /**
     * Conditionally approves [expectedEmail] for [id] and emits exactly one update after commit.
     *
     * The SQL predicate includes both identity and the stored address, so a stale verification link
     * cannot approve a later replacement. Repeating a valid approval returns the stored approved row
     * and remains idempotent; an absent user or mismatched/cleared address emits no event.
     *
     * @param id User whose current email is being approved.
     * @param expectedEmail Address bound to the verification link.
     * @return The approved user, or `null` when the conditional predicate did not match.
     */
    override suspend fun approveEmail(id: UserId, expectedEmail: Email, cooldownMillis: Long): RegisteredUser? {
        val approval = transaction(db = database) {
            acquireWriteLock()
            val current = selectUser(id)
                ?: return@transaction null
            when {
                current.pendingEmail == expectedEmail -> {
                    val deadline = if (cooldownMillis == 0L) null else Math.addExact(nowMillis(), cooldownMillis)
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                        it[emailColumn] = expectedEmail.string
                        it[pendingEmailColumn] = null
                        it[emailApprovedColumn] = true
                        it[emailChangeAllowedAtColumn] = deadline
                    }
                    selectUser(id)?.let { true to it }
                }
                current.email == expectedEmail && !current.emailApproved && current.pendingEmail == null -> {
                    val deadline = if (cooldownMillis == 0L) null else Math.addExact(nowMillis(), cooldownMillis)
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                        it[emailApprovedColumn] = true
                        it[emailChangeAllowedAtColumn] = deadline
                    }
                    selectUser(id)?.let { true to it }
                }
                current.email == expectedEmail && current.emailApproved && current.pendingEmail == null -> false to current
                else -> null
            }
        }
        val approved = approval?.second
        if (approval?.first == true && approved != null) {
            _updatedObjectsFlow.emit(approved)
        }
        return approved
    }

    init {
        initTable()
        transaction(db = database) {
            SchemaUtils.createMissingTablesAndColumns(UsersWriteLockTable)
            UsersWriteLockTable.insertIgnore {
                it[UsersWriteLockTable.idColumn] = usersWriteLockRowId
                it[UsersWriteLockTable.markerColumn] = usersWriteLockMarker
            }
        }
    }

    /** Performs a lifecycle-aware email mutation, optionally applying a username replacement. */
    private suspend fun mutateEmail(id: UserId, email: Email?, username: Username?): RegisteredUser? {
        val updated = try {
            transaction(db = database) {
                acquireWriteLock()
                mutateEmailInTransaction(id = id, email = email, username = username)
            }
        } catch (error: ExposedSQLException) {
            if (error.isUniqueViolation()) throw DuplicateUserFieldException(cause = error) else throw error
        }
        updated?.let { _updatedObjectsFlow.emit(it) }
        return updated
    }

    /** Applies a lifecycle mutation after the caller has acquired [UsersWriteLockTable]'s row. */
    private fun JdbcTransaction.mutateEmailInTransaction(
        id: UserId,
        email: Email?,
        username: Username?,
    ): RegisteredUser? {
        val current = selectUser(id) ?: return null
        val changingEmail = email != current.email && email != current.pendingEmail
        val deadline = current.emailChangeAllowedAt
        if (changingEmail && deadline != null && nowMillis() < deadline) {
            throw EmailChangeCooldownException(deadline)
        }
        if (changingEmail) ensureEmailAvailable(email = email, excludedId = id)
        when {
            !changingEmail -> {
                if (username != null && username != current.username) {
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) { it[usernameColumn] = username.string }
                }
            }
            email == null -> {
                this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                    if (username != null) it[usernameColumn] = username.string
                    it[emailColumn] = null
                    it[pendingEmailColumn] = null
                    it[emailApprovedColumn] = false
                    it[emailChangeAllowedAtColumn] = null
                }
            }
            current.emailApproved && current.email != null -> {
                this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                    if (username != null) it[usernameColumn] = username.string
                    it[pendingEmailColumn] = email.string
                }
            }
            else -> {
                this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                    if (username != null) it[usernameColumn] = username.string
                    it[emailColumn] = email.string
                    it[pendingEmailColumn] = null
                    it[emailApprovedColumn] = false
                    it[emailChangeAllowedAtColumn] = null
                }
            }
        }
        return selectUser(id)
    }

    /** Updates the durable lock row before any users-table query. */
    private fun JdbcTransaction.acquireWriteLock() {
        val acquired = UsersWriteLockTable.exposedUpdate({ UsersWriteLockTable.idColumn eq usersWriteLockRowId }) {
            it[UsersWriteLockTable.markerColumn] = usersWriteLockMarker
        }
        check(acquired == 1) { "users_write_lock row id=$usersWriteLockRowId is missing" }
    }

    /** Finds a users row without routing its raw columns through a cache or another transaction. */
    private fun JdbcTransaction.selectUser(id: UserId): RegisteredUser? =
        selectAll().where { idColumn eq id.long }.limit(1).firstOrNull()?.asObject

    /** Rejects addresses occupied in either raw persistent email slot. */
    private fun JdbcTransaction.ensureEmailAvailable(
        email: Email?,
        excludedId: UserId?,
    ) {
        if (email == null) return
        val ownership = (emailColumn eq email.string) or (pendingEmailColumn eq email.string)
        val predicate = if (excludedId == null) ownership else (idColumn neq excludedId.long) and ownership
        if (selectAll().where { predicate }.limit(1).any()) throw DuplicateUserFieldException()
    }

    private companion object {
        /** Stable lock-row identity. */
        const val usersWriteLockRowId = 1

        /** Stable lock-row marker. */
        const val usersWriteLockMarker = 0
    }
}
