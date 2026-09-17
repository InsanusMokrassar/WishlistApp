package dev.inmo.wishlist.features.users.common.repo

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
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update as exposedUpdate

/**
 * Exposed-backed PostgreSQL and SQLite implementation of [UsersRepo].
 *
 * Stores users in the `users` table with an auto-increment `id`, a unique `username`, and a
 * unique, nullable `email` column. The nullable `email` column is additive — `initTable()` adds
 * it to any existing table via `createMissingTablesAndColumns`, keeping existing rows valid with
 * `NULL`; `NULL` values are exempt from the uniqueness check, so users without a stored email
 * never collide with each other.
 *
 * [update] and [create] translate an exact PostgreSQL or SQLite unique-violation on either unique
 * column into [DuplicateUserFieldException] (see [isUniqueViolation]) instead of letting the raw
 * [ExposedSQLException] escape. Other constraint and database failures retain their original type.
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

    override suspend fun setEmail(id: UserId, email: Email?): RegisteredUser? {
        val updated = transaction(db = database) {
            val current = selectAll().where { idColumn eq id.long }.limit(1).firstOrNull()?.asObject
                ?: return@transaction null
            val changing = when {
                email == current.email || email == current.pendingEmail -> false
                else -> true
            }
            val deadline = current.emailChangeAllowedAt
            if (changing && deadline != null && nowMillis() < deadline) throw EmailChangeCooldownException(deadline)
            if (email != null && changing) {
                val occupied = selectAll().where {
                    (idColumn neq id.long) and ((emailColumn eq email.string) or (pendingEmailColumn eq email.string))
                }.limit(1).any()
                if (occupied) throw DuplicateUserFieldException()
            }
            when {
                !changing -> current
                email == null -> {
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                        it[emailColumn] = null
                        it[pendingEmailColumn] = null
                        it[emailApprovedColumn] = false
                        it[emailChangeAllowedAtColumn] = null
                    }
                    selectAll().where { idColumn eq id.long }.limit(1).first().asObject
                }
                current.emailApproved && current.email != null -> {
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) { it[pendingEmailColumn] = email.string }
                    selectAll().where { idColumn eq id.long }.limit(1).first().asObject
                }
                else -> {
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                        it[emailColumn] = email.string
                        it[pendingEmailColumn] = null
                        it[emailApprovedColumn] = false
                    }
                    selectAll().where { idColumn eq id.long }.limit(1).first().asObject
                }
            }
        }
        updated?.let { _updatedObjectsFlow.emit(it) }
        return updated
    }

    override suspend fun updateUsername(id: UserId, username: Username): RegisteredUser? {
        val updated = transaction(db = database) {
            this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) { it[usernameColumn] = username.string }
            selectAll().where { idColumn eq id.long }.limit(1).firstOrNull()?.asObject
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
        try {
            super.update(id, value)
        } catch (e: ExposedSQLException) {
            if (e.isUniqueViolation()) throw DuplicateUserFieldException(cause = e) else throw e
        }

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
    override suspend fun create(values: List<NewUser>): List<RegisteredUser> =
        try {
            super.create(values)
        } catch (e: ExposedSQLException) {
            if (e.isUniqueViolation()) throw DuplicateUserFieldException(cause = e) else throw e
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
        val approved = transaction(db = database) {
            val current = selectAll().where { idColumn eq id.long }.limit(1).firstOrNull()?.asObject
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
                    selectAll().where { idColumn eq id.long }.limit(1).first().asObject
                }
                current.email == expectedEmail && !current.emailApproved && current.pendingEmail == null -> {
                    val deadline = if (cooldownMillis == 0L) null else Math.addExact(nowMillis(), cooldownMillis)
                    this@ExposedUsersRepo.exposedUpdate({ idColumn eq id.long }) {
                        it[emailApprovedColumn] = true
                        it[emailChangeAllowedAtColumn] = deadline
                    }
                    selectAll().where { idColumn eq id.long }.limit(1).first().asObject
                }
                current.email == expectedEmail && current.emailApproved && current.pendingEmail == null -> current
                else -> null
            }
        }
        if (approved != null) {
            _updatedObjectsFlow.emit(approved)
        }
        return approved
    }

    init {
        initTable()
    }
}
