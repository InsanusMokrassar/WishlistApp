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
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.booleanLiteral
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
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

    /**
     * Durable approval evidence for [emailColumn].
     *
     * The non-null default makes the additive migration conservative: every historic row starts
     * pending instead of inferring mailbox ownership from a role or a previously stored address.
     */
    private val emailApprovedColumn = bool("email_approved").default(false)

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
                emailApproved = email != null && get(emailApprovedColumn)
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
    override suspend fun approveEmail(id: UserId, expectedEmail: Email): RegisteredUser? {
        val approved = transaction(db = database) {
            val predicate = (idColumn eq id.long) and (emailColumn eq expectedEmail.string)
            val changed = this@ExposedUsersRepo.exposedUpdate({ predicate }) {
                it[emailApprovedColumn] = true
            }
            if (changed == 0) {
                null
            } else {
                selectAll().where { predicate }.limit(1).firstOrNull()?.asObject
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
