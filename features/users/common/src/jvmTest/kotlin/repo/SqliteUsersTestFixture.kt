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
import java.util.concurrent.atomic.AtomicBoolean
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
internal class SqliteBusyObservation(
    /** Absolute callback-owned retry window measured from the first busy callback. */
    private val retryTimeoutNanos: Long = TimeUnit.SECONDS.toNanos(15),
) {
    private val activeSql = AtomicReference<String?>(null)
    private val callbackFailure = AtomicReference<Throwable?>(null)
    private val retryGate = CountDownLatch(1)
    private val busyEntry = CountDownLatch(1)
    private val retryDeadlineNanos = AtomicReference<Long?>(null)
    private val retryDeadlineExhausted = AtomicBoolean(false)
    private val retryDeadlineExhaustion = CountDownLatch(1)

    init {
        require(retryTimeoutNanos >= 0) { "SQLite busy retry timeout must not be negative" }
    }

    fun install(connection: Connection) {
        BusyHandler.setHandler(connection, object : BusyHandler() {
            override fun callback(nbPrevInvok: Int): Int {
                val sql = activeSql.get()
                if (sql == null || !sql.contains("users_write_lock", ignoreCase = true) || !sql.contains("UPDATE", ignoreCase = true)) {
                    callbackFailure.compareAndSet(null, AssertionError("SQLite busy callback was not entered by users_write_lock UPDATE: $sql"))
                    return 0
                }
                busyEntry.countDown()
                return retryAfterGateBeforeDeadline()
            }
        })
    }

    /** Waits for one release gate while ensuring every native retry remains inside one absolute deadline. */
    private fun retryAfterGateBeforeDeadline(): Int {
        val deadline = retryDeadlineNanos.updateAndGet { existing ->
            existing ?: System.nanoTime() + retryTimeoutNanos
        } ?: error("SQLite busy retry deadline was not initialized")
        val remainingNanos = deadline - System.nanoTime()
        if (remainingNanos <= 0) return recordRetryDeadlineExhaustion()
        if (!retryGate.await(remainingNanos, TimeUnit.NANOSECONDS)) return recordRetryDeadlineExhaustion()
        return if (System.nanoTime() < deadline) 1 else recordRetryDeadlineExhaustion()
    }

    /** Records the terminal callback decision so a timed-out contention proof cannot pass as healthy. */
    private fun recordRetryDeadlineExhaustion(): Int {
        retryDeadlineExhausted.set(true)
        retryDeadlineExhaustion.countDown()
        return 0
    }

    /** Marks SQL from prepare through statement close so native callbacks during either phase retain their real target. */
    fun beginPreparedStatement(sql: String) {
        activeSql.set(sql)
    }

    /** Clears SQL only when the prepared statement has completed its JDBC lifetime. */
    fun endPreparedStatement(sql: String) {
        activeSql.compareAndSet(sql, null)
    }

    fun awaitBusyEntry(): Boolean = busyEntry.await(10, TimeUnit.SECONDS)

    /** Waits for the callback-owned deadline to reject another native SQLite retry. */
    fun awaitRetryDeadlineExhaustion(): Boolean = retryDeadlineExhaustion.await(10, TimeUnit.SECONDS)

    fun releaseRetry() {
        retryGate.countDown()
    }

    fun assertHealthy() {
        callbackFailure.get()?.let { throw it }
        check(!retryDeadlineExhausted.get()) { "SQLite busy retry deadline expired before contention completed" }
    }

    /** Verifies that a direct bounded-retry proof reached the callback-owned terminal decision. */
    fun assertRetryDeadlineExhausted() {
        callbackFailure.get()?.let { throw it }
        check(retryDeadlineExhausted.get()) { "SQLite busy retry deadline did not expire" }
    }
}

/** Wraps statement preparation and execution so the native busy callback can identify the active SQL. */
private fun recordingSqliteConnection(connection: Connection, observation: SqliteBusyObservation): Connection {
    observation.install(connection)
    val handler = InvocationHandler { _, method, arguments ->
        if (method.name == "prepareStatement" && arguments?.firstOrNull() is String) {
            val sql = arguments.first() as String
            observation.beginPreparedStatement(sql)
            try {
                val result = invokeJdbc(connection, method, arguments)
                if (result is PreparedStatement) recordingPreparedStatement(result, sql, observation) else {
                    observation.endPreparedStatement(sql)
                    result
                }
            } catch (error: Throwable) {
                observation.endPreparedStatement(sql)
                throw error
            }
        } else {
            invokeJdbc(connection, method, arguments)
        }
    }
    return Proxy.newProxyInstance(Connection::class.java.classLoader, arrayOf(Connection::class.java), handler) as Connection
}

/** Preserves JDBC exceptions while clearing prepared SQL only after native statement close. */
private fun recordingPreparedStatement(
    statement: PreparedStatement,
    sql: String,
    observation: SqliteBusyObservation,
): PreparedStatement {
    val handler = InvocationHandler { _, method, arguments ->
        when (method.name) {
            "close" -> try {
                invokeJdbc(statement, method, arguments)
            } finally {
                observation.endPreparedStatement(sql)
            }
            else -> invokeJdbc(statement, method, arguments)
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
