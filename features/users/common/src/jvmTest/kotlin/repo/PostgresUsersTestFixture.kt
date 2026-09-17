package dev.inmo.wishlist.features.users.common.repo

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.DriverManager
import java.util.UUID

/**
 * Runs PostgreSQL tests in a generated schema that the fixture creates and drops itself.
 *
 * The JDBC URL must address a disposable database supplied solely through
 * `WISHLIST_POSTGRES_TEST_JDBC_URL`; the fixture never selects, drops, or mutates a pre-existing
 * application schema.
 */
internal suspend fun withPostgresUsersRepo(block: suspend (ExposedUsersRepo) -> Unit) {
    val url = checkNotNull(System.getenv("WISHLIST_POSTGRES_TEST_JDBC_URL")) {
        "WISHLIST_POSTGRES_TEST_JDBC_URL is required for PostgreSQL lifecycle tests"
    }
    val schema = "wishlist_users_${UUID.randomUUID().toString().replace('-', '_')}"
    DriverManager.getConnection(url).use { connection ->
        connection.createStatement().use { statement -> statement.execute("CREATE SCHEMA $schema") }
    }
    val schemaUrl = "$url${if ('?' in url) '&' else '?'}currentSchema=$schema"
    val database = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
    try {
        block(ExposedUsersRepo(database))
    } finally {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement -> statement.execute("DROP SCHEMA $schema CASCADE") }
            }
        }
    }
}
