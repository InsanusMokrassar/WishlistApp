package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import java.sql.DriverManager
import java.sql.ResultSet

/** Instrumented real in-memory key-value store counting every cache publication.
 * @param storage Delegated in-memory key-value repository.
 */
internal class CountingUsersCache(
    private val storage: KeyValueRepo<UserId, RegisteredUser> = MapKeyValueRepo(),
) : KeyValueRepo<UserId, RegisteredUser> by storage {
    /** Number of batch cache set calls. */
    var setCalls: Int = 0
        private set

    override suspend fun set(toSet: Map<UserId, RegisteredUser>) {
        setCalls++
        storage.set(toSet)
    }
}

/** Complete storage row for assertions that must bypass email-profile decoding and the users cache.
 * @param id Durable row id.
 * @param username Stored login name.
 * @param email Stored current address.
 * @param emailApproved Stored current-address approval flag.
 * @param pendingEmail Stored replacement address.
 * @param requestedAt Nullable raw request instant.
 * @param allowedAt Nullable raw cooldown deadline.
 */
internal data class RawUsersRow(
    val id: Long,
    val username: String,
    val email: String?,
    val emailApproved: Boolean,
    val pendingEmail: String?,
    val requestedAt: Long?,
    val allowedAt: Long?,
)

/** Reads every user row through an independent JDBC connection, preserving nullable BIGINT values. */
internal fun rawUsersSnapshot(url: String): List<RawUsersRow> = DriverManager.getConnection(url).use { connection ->
    connection.createStatement().use { statement ->
        statement.executeQuery(
            "SELECT id, username, email, email_approved, pending_email, email_change_requested_at, email_change_allowed_at FROM users ORDER BY id"
        ).use { rows ->
            buildList {
                while (rows.next()) {
                    add(RawUsersRow(
                        id = rows.getLong("id"),
                        username = rows.getString("username"),
                        email = rows.getString("email"),
                        emailApproved = rows.getBoolean("email_approved"),
                        pendingEmail = rows.getString("pending_email"),
                        requestedAt = rows.nullableLong("email_change_requested_at"),
                        allowedAt = rows.nullableLong("email_change_allowed_at"),
                    ))
                }
            }
        }
    }
}

/** Converts one JDBC BIGINT to nullable Long without conflating SQL NULL with epoch zero. */
private fun ResultSet.nullableLong(column: String): Long? {
    val value = getLong(column)
    return if (wasNull()) null else value
}

/** Counts invalid requested values, invalid allowed values, and distinct affected rows. */
internal fun invalidEmailTimestampCounts(url: String): Triple<Long, Long, Long> =
    DriverManager.getConnection(url).use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT
                  COUNT(CASE WHEN email_change_requested_at < -4503599627370496 OR email_change_requested_at > 4503599627370496 THEN 1 END),
                  COUNT(CASE WHEN email_change_allowed_at < -4503599627370496 OR email_change_allowed_at > 4503599627370496 THEN 1 END),
                  COUNT(CASE WHEN (email_change_requested_at < -4503599627370496 OR email_change_requested_at > 4503599627370496)
                             OR (email_change_allowed_at < -4503599627370496 OR email_change_allowed_at > 4503599627370496) THEN 1 END)
                FROM users
                """.trimIndent()
            ).use { rows ->
                check(rows.next())
                Triple(rows.getLong(1), rows.getLong(2), rows.getLong(3))
            }
        }
    }
