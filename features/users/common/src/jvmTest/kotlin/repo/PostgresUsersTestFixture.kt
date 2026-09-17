package dev.inmo.wishlist.features.users.common.repo

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.postgresql.PGConnection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    withBoundedPostgresFixtureStatement(url) { statement -> statement.execute("CREATE SCHEMA \"$schema\"") }
    val schemaUrl = "$url${if ('?' in url) '&' else '?'}currentSchema=$schema"
    var primaryFailure: Throwable? = null
    try {
        block(schemaUrl)
    } catch (error: Throwable) {
        primaryFailure = error
        throw error
    } finally {
        val cleanupFailure = runCatching {
            withBoundedPostgresFixtureStatement(url) { statement -> statement.execute("DROP SCHEMA \"$schema\" CASCADE") }
        }.exceptionOrNull()
        if (cleanupFailure != null) {
            primaryFailure?.addSuppressed(cleanupFailure)
            if (primaryFailure == null) throw cleanupFailure
        }
    }
}

/** Opens a task-owned PostgreSQL connection with finite driver, network, server, and JDBC statement bounds. */
private fun openBoundedPostgresFixtureConnection(url: String): java.sql.Connection {
    val connection = DriverManager.getConnection(postgresFixtureUrlWithDriverBounds(url))
    return try {
        connection.setNetworkTimeout(postgresFixtureNetworkTimeoutExecutor, postgresFixtureTimeoutMillis)
        connection.createStatement().use { statement ->
            statement.queryTimeout = postgresFixtureTimeoutSeconds
            statement.execute("SET statement_timeout = '${postgresFixtureTimeoutMillis}ms'")
            statement.execute("SET lock_timeout = '${postgresFixtureTimeoutMillis}ms'")
        }
        connection
    } catch (error: Throwable) {
        runCatching(connection::close).exceptionOrNull()?.let(error::addSuppressed)
        throw error
    }
}

/** Runs a schema-setup or schema-cleanup statement with the fixture's bounded connection and statement settings. */
private inline fun <T> withBoundedPostgresFixtureStatement(url: String, block: (java.sql.Statement) -> T): T =
    openBoundedPostgresFixtureConnection(url).use { connection ->
        connection.createStatement().use { statement ->
            statement.queryTimeout = postgresFixtureTimeoutSeconds
            block(statement)
        }
    }

/** Adds finite PostgreSQL driver limits without replacing the task-owned database URL parameters. */
private fun postgresFixtureUrlWithDriverBounds(url: String): String =
    "$url${if ('?' in url) '&' else '?'}connectTimeout=5&loginTimeout=5&socketTimeout=25&tcpKeepAlive=true"

/** Supplies daemon timeout tasks so an unsuccessful JDBC timeout cannot retain a non-daemon worker. */
private val postgresFixtureNetworkTimeoutExecutor = Executor { command ->
    Thread(command, "PostgreSQL fixture network timeout").apply { isDaemon = true }.start()
}

/** Shared finite server, JDBC, and network timeout for outer PostgreSQL fixture schema operations. */
private const val postgresFixtureTimeoutMillis = 25_000

/** Converts the fixture timeout into the seconds unit required by JDBC statement timeouts. */
private const val postgresFixtureTimeoutSeconds = 25

/**
 * Runs [block] with two independent Exposed databases and repositories sharing one fixture-owned schema.
 *
 * @param firstAfterWriteLock Friend-test callback for the first repository after singleton-lock acquisition.
 * @param secondAfterWriteLock Friend-test callback for the second repository after singleton-lock acquisition.
 * @param captureBackendPids When armed, records the physical worker backend PIDs without issuing SQL.
 * @param block Test body receiving the schema JDBC URL and independent repositories.
 */
internal suspend fun withPostgresUsersRepos(
    firstNowMillis: () -> Long = System::currentTimeMillis,
    firstAfterWriteLock: (() -> Unit)? = null,
    secondNowMillis: () -> Long = System::currentTimeMillis,
    secondAfterWriteLock: (() -> Unit)? = null,
    captureBackendPids: PostgresBackendPids? = null,
    block: suspend (String, ExposedUsersRepo, ExposedUsersRepo) -> Unit,
) = withPostgresUsersSchema { schemaUrl ->
    val firstDatabase = Database.connect(
        url = schemaUrl,
        driver = "org.postgresql.Driver",
        setupConnection = { connection -> captureBackendPids?.captureFirst(connection) },
    )
    val secondDatabase = Database.connect(
        url = schemaUrl,
        driver = "org.postgresql.Driver",
        setupConnection = { connection -> captureBackendPids?.captureSecond(connection) },
    )
    try {
        block(
            schemaUrl,
            ExposedUsersRepo(firstDatabase, firstNowMillis, firstAfterWriteLock),
            ExposedUsersRepo(secondDatabase, secondNowMillis, secondAfterWriteLock),
        )
    } finally {
        try {
            TransactionManager.closeAndUnregister(firstDatabase)
        } finally {
            TransactionManager.closeAndUnregister(secondDatabase)
        }
    }
}

/** Captures worker backend PIDs only after a contention test arms the fixture. */
internal class PostgresBackendPids {
    private val armed = AtomicBoolean(false)
    private val first = AtomicInteger(-1)
    private val second = AtomicInteger(-1)

    fun arm() {
        first.set(-1)
        second.set(-1)
        armed.set(true)
    }

    fun firstPid(): Int = first.get()

    fun secondPid(): Int = second.get()

    fun captureFirst(connection: java.sql.Connection) {
        if (armed.get()) first.set(connection.unwrap(PGConnection::class.java).backendPID)
    }

    fun captureSecond(connection: java.sql.Connection) {
        if (armed.get()) second.set(connection.unwrap(PGConnection::class.java).backendPID)
    }
}
