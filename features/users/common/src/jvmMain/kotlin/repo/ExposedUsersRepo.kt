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
