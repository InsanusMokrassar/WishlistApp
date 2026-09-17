package dev.inmo.wishlist.features.users.common.repo

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.sqlite.BusyHandler
import java.sql.DriverManager
import java.sql.Connection
import java.sql.PreparedStatement
import java.nio.file.Files
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Runs [block] against an isolated in-memory SQLite users repository using [nowMillis] for lifecycle deadlines. */
internal suspend fun withInMemorySqliteUsersRepo(
    nowMillis: () -> Long = System::currentTimeMillis,
    block: suspend (ExposedUsersRepo) -> Unit,
) {
    val url = "jdbc:sqlite:file:users-${UUID.randomUUID()}?mode=memory&cache=shared"
    val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
    val keeper = try {
        DriverManager.getConnection(url)
    } catch (error: Throwable) {
        TransactionManager.closeAndUnregister(database)
        throw error
    }

    try {
        block(ExposedUsersRepo(database, nowMillis))
    } finally {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            keeper.close()
        }
    }
}

/**
 * Runs [block] with independent Exposed databases and repositories connected to one fixture-owned SQLite file.
 *
 * @param firstNowMillis Clock supplied to the first repository.
 * @param firstAfterWriteLock Barrier callback invoked by the first repository immediately after lock acquisition.
 * @param secondNowMillis Clock supplied to the second repository.
 * @param secondAfterWriteLock Barrier callback invoked by the second repository immediately after lock acquisition.
 * @param block Test body receiving the file JDBC URL and the independent repositories.
 */
internal suspend fun withFileBackedSqliteUsersRepos(
    firstNowMillis: () -> Long = System::currentTimeMillis,
    firstAfterWriteLock: (() -> Unit)? = null,
    secondNowMillis: () -> Long = System::currentTimeMillis,
    secondAfterWriteLock: (() -> Unit)? = null,
    secondBusyObservation: SqliteBusyObservation? = null,
    block: suspend (String, ExposedUsersRepo, ExposedUsersRepo) -> Unit,
) {
    val databaseFile = Files.createTempFile("wishlist-users-concurrent", ".sqlite")
    val url = "jdbc:sqlite:file:${databaseFile.toAbsolutePath()}?busy_timeout=5000"
    val firstDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
    val secondDatabase = if (secondBusyObservation == null) {
        Database.connect(url = url, driver = "org.sqlite.JDBC")
    } else {
        Database.connect(getNewConnection = {
            recordingSqliteConnection(
                connection = DriverManager.getConnection(url),
                observation = secondBusyObservation,
            )
        })
    }
    try {
        block(
            url,
            ExposedUsersRepo(firstDatabase, firstNowMillis, firstAfterWriteLock),
            ExposedUsersRepo(secondDatabase, secondNowMillis, secondAfterWriteLock),
        )
    } finally {
        try {
            TransactionManager.closeAndUnregister(firstDatabase)
        } finally {
            try {
                TransactionManager.closeAndUnregister(secondDatabase)
            } finally {
                Files.deleteIfExists(databaseFile)
            }
        }
    }
}

/** Installed Xerial callback and active-statement recorder for a single contender connection. */
internal class SqliteBusyObservation {
    private val activeSql = AtomicReference<String?>(null)
    private val callbackFailure = AtomicReference<Throwable?>(null)
    private val retryGate = CountDownLatch(1)
    private val busyEntry = CountDownLatch(1)

    fun install(connection: Connection) {
        BusyHandler.setHandler(connection, object : BusyHandler() {
            override fun callback(nbPrevInvok: Int): Int {
                val sql = activeSql.get()
                if (sql == null || !sql.contains("users_write_lock", ignoreCase = true) || !sql.contains("UPDATE", ignoreCase = true)) {
                    callbackFailure.compareAndSet(null, AssertionError("SQLite busy callback was not entered by users_write_lock UPDATE: $sql"))
                    return 0
                }
                busyEntry.countDown()
                return if (retryGate.await(15, TimeUnit.SECONDS)) 1 else 0
            }
        })
    }

    fun record(sql: String, action: () -> Any?): Any? {
        activeSql.set(sql)
        return try {
            action()
        } finally {
            activeSql.compareAndSet(sql, null)
        }
    }

    fun awaitBusyEntry(): Boolean = busyEntry.await(10, TimeUnit.SECONDS)

    fun releaseRetry() {
        retryGate.countDown()
    }

    fun assertHealthy() {
        callbackFailure.get()?.let { throw it }
    }
}

/** Wraps only prepared-statement execution so the native busy callback can identify the active SQL. */
private fun recordingSqliteConnection(connection: Connection, observation: SqliteBusyObservation): Connection {
    observation.install(connection)
    val handler = InvocationHandler { _, method, arguments ->
        val result = invokeJdbc(connection, method, arguments)
        if (method.name == "prepareStatement" && result is PreparedStatement && arguments?.firstOrNull() is String) {
            recordingPreparedStatement(result, arguments.first() as String, observation)
        } else {
            result
        }
    }
    return Proxy.newProxyInstance(Connection::class.java.classLoader, arrayOf(Connection::class.java), handler) as Connection
}

/** Preserves JDBC exceptions while marking execute variants as active around the native driver call. */
private fun recordingPreparedStatement(
    statement: PreparedStatement,
    sql: String,
    observation: SqliteBusyObservation,
): PreparedStatement {
    val handler = InvocationHandler { _, method, arguments ->
        if (method.name in setOf("execute", "executeUpdate", "executeLargeUpdate")) {
            observation.record(sql) { invokeJdbc(statement, method, arguments) }
        } else {
            invokeJdbc(statement, method, arguments)
        }
    }
    return Proxy.newProxyInstance(PreparedStatement::class.java.classLoader, arrayOf(PreparedStatement::class.java), handler) as PreparedStatement
}

/** Invokes a JDBC proxy target while retaining its original checked exception rather than reflection wrapping. */
private fun invokeJdbc(target: Any, method: java.lang.reflect.Method, arguments: Array<out Any?>?): Any? =
    try {
        method.invoke(target, *(arguments ?: emptyArray()))
    } catch (error: InvocationTargetException) {
        throw error.targetException
    }
