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
internal suspend fun withPostgresUsersRepo(block: suspend (ExposedUsersRepo) -> Unit) =
    withPostgresUsersSchema { schemaUrl ->
        val database = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
        try {
            block(ExposedUsersRepo(database))
        } finally {
            TransactionManager.closeAndUnregister(database)
        }
    }

/**
 * Runs [block] in an otherwise-empty generated PostgreSQL schema and removes only that schema.
 *
 * @param block Test body receiving a JDBC URL whose `currentSchema` is fixture-owned.
 */
internal suspend fun withPostgresUsersSchema(block: suspend (String) -> Unit) {
    val url = checkNotNull(System.getenv("WISHLIST_POSTGRES_TEST_JDBC_URL")) {
        "WISHLIST_POSTGRES_TEST_JDBC_URL is required for PostgreSQL lifecycle tests"
    }
    val schema = "wishlist_users_${UUID.randomUUID().toString().replace('-', '_')}"
    DriverManager.getConnection(url).use { connection ->
        connection.createStatement().use { statement -> statement.execute("CREATE SCHEMA $schema") }
    }
    val schemaUrl = "$url${if ('?' in url) '&' else '?'}currentSchema=$schema"
    try {
        block(schemaUrl)
    } finally {
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement -> statement.execute("DROP SCHEMA $schema CASCADE") }
        }
    }
}

/**
 * Runs [block] with two independent Exposed databases and repositories sharing one fixture-owned schema.
 *
 * @param firstAfterWriteLock Callback for the first repository immediately after singleton-lock acquisition.
 * @param secondAfterWriteLock Callback for the second repository immediately after singleton-lock acquisition.
 * @param block Test body receiving the schema JDBC URL and independent repositories.
 */
internal suspend fun withPostgresUsersRepos(
    firstAfterWriteLock: (() -> Unit)? = null,
    secondAfterWriteLock: (() -> Unit)? = null,
    block: suspend (String, ExposedUsersRepo, ExposedUsersRepo) -> Unit,
) = withPostgresUsersSchema { schemaUrl ->
    val firstDatabase = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
    val secondDatabase = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
    try {
        block(
            schemaUrl,
            ExposedUsersRepo(firstDatabase, afterWriteLock = firstAfterWriteLock),
            ExposedUsersRepo(secondDatabase, afterWriteLock = secondAfterWriteLock),
        )
    } finally {
        try {
            TransactionManager.closeAndUnregister(firstDatabase)
        } finally {
            TransactionManager.closeAndUnregister(secondDatabase)
        }
    }
}
