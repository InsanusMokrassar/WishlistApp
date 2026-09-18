package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies the email lifecycle against a disposable PostgreSQL schema. */
class PostgresUsersRepoTest {
    /** PostgreSQL writers retain approval, protect both slots, and atomically promote the candidate. */
    @Test
    fun retainedApprovalAndCrossSlotUniqueness() = runTest {
        withPostgresUsersRepo { repo ->
            val current = Email("postgres-current@example.com")
            val pending = Email("postgres-pending@example.com")
            val user = repo.create(NewUser(Username("postgres-owner"), current)).single()
            checkNotNull(repo.approveEmail(user.id, current, cooldownMillis = 0))

            val replacement = checkNotNull(repo.setEmail(user.id, pending))
            assertEquals(current, replacement.email)
            assertEquals(pending, replacement.pendingEmail)
            assertTrue(replacement.emailApproved)

            val duplicate = assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("postgres-other"), pending))
            }
            assertNull(duplicate.cause)

            val promoted = checkNotNull(repo.approveEmail(user.id, pending, cooldownMillis = 1_000))
            assertEquals(pending, promoted.email)
            assertNull(promoted.pendingEmail)
            assertTrue(promoted.emailApproved)
            assertNotNull(promoted.emailChangeAllowedAt)
        }
    }

    /** PostgreSQL uses the same guarded clear lifecycle for dedicated, generic, and bulk writes. */
    @Test
    fun clearLifecycleHonorsDeadlineAcrossMutationEntryPoints() = runTest {
        var now = 1_000L
        withPostgresUsersSchema { schemaUrl ->
            val database = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
            try {
                val repo = ExposedUsersRepo(database, nowMillis = { now })
                val address = Email("postgres-clear@example.com")
                val user = repo.create(NewUser(Username("postgres-clear"), address)).single()
                checkNotNull(repo.approveEmail(user.id, address, cooldownMillis = 500L))

                assertFailsWith<EmailChangeCooldownException> { repo.setEmail(user.id, null) }
                assertFailsWith<EmailChangeCooldownException> {
                    repo.update(user.id, NewUser(Username("must-not-rename"), null))
                }
                assertEquals(Username("postgres-clear"), repo.getById(user.id)?.username)

                now = 1_500L
                val cleared = checkNotNull(repo.update(listOf(user.id to NewUser(Username("postgres-cleared"), null))).single())
                assertEquals(Username("postgres-cleared"), cleared.username)
                assertNull(cleared.email)
                assertNull(cleared.pendingEmail)
                assertFalse(cleared.emailApproved)
                assertNull(cleared.emailChangeAllowedAt)
                openBoundedPostgresConnection(schemaUrl).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT email, pending_email, email_approved, email_change_allowed_at FROM users WHERE id = ${user.id.long}").use { row ->
                            assertTrue(row.next())
                            assertNull(row.getString("email"))
                            assertNull(row.getString("pending_email"))
                            assertFalse(row.getBoolean("email_approved"))
                            assertNull(row.getObject("email_change_allowed_at"))
                        }
                    }
                }
            } finally {
                TransactionManager.closeAndUnregister(database)
            }
        }
    }

    /** Independent PostgreSQL writers serialize current-address claims before the second lifecycle read. */
    @Test
    fun independentRepositoriesSerializeCurrentAddressClaimsBeforeSecondLifecycleRead() = runBlockingPostgresTest {
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondLockAcquired = CountDownLatch(1)
        val backendPids = PostgresBackendPids()
        withBoundedPostgresUsersRepos(
            firstAfterWriteLock = {
                firstLockAcquired.countDown()
                check(releaseFirst.await(15, TimeUnit.SECONDS)) { "PostgreSQL first current-address worker was not released" }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
            captureBackendPids = backendPids,
        ) { schemaUrl, first, second ->
            val shared = Email("postgres-concurrent-current@example.com")
            val firstWorker = BoundedPostgresTestWorker("PostgreSQL first current-address worker") {
                first.create(NewUser(Username("postgres-current-first"), shared)).single()
            }
            val secondWorker = BoundedPostgresTestWorker("PostgreSQL second current-address worker") {
                second.create(NewUser(Username("postgres-current-second"), shared)).single()
            }
            backendPids.arm()
            runCleanupProtectedPostgresContention(
                workers = listOf(firstWorker, secondWorker),
                cleanupActions = listOf(releaseFirst::countDown),
            ) {
                firstWorker.start()
                assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
                secondWorker.start()
                assertPostgresContenderBlockedOnUsersWriteLock(schemaUrl, backendPids)
                assertEquals(1, secondLockAcquired.count)
            }
            assertEquals(shared, firstWorker.result().getOrThrow().email)
            assertIs<DuplicateUserFieldException>(secondWorker.result().exceptionOrNull())
            assertRawAddressHasOneClaim(schemaUrl, shared)
            assertNoRawSharedAddress(schemaUrl)
        }
    }

    /** A pending writer and a current writer cannot commit the same raw address across repository instances. */
    @Test
    fun independentRepositoriesSerializePendingAndCurrentAddressClaims() = runBlockingPostgresTest {
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondLockAcquired = CountDownLatch(1)
        val blockFirstWriter = AtomicBoolean(false)
        val backendPids = PostgresBackendPids()
        withBoundedPostgresUsersRepos(
            firstAfterWriteLock = {
                if (blockFirstWriter.get()) {
                    firstLockAcquired.countDown()
                    check(releaseFirst.await(15, TimeUnit.SECONDS)) { "PostgreSQL pending-address worker was not released" }
                }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
            captureBackendPids = backendPids,
        ) { schemaUrl, first, second ->
            val current = Email("postgres-pending-owner@example.com")
            val pending = Email("postgres-pending-contended@example.com")
            val owner = first.create(NewUser(Username("postgres-pending-owner"), current)).single()
            checkNotNull(first.approveEmail(owner.id, current))
            blockFirstWriter.set(true)

            val pendingWorker = BoundedPostgresTestWorker("PostgreSQL pending-address worker") {
                first.setEmail(owner.id, pending)
            }
            val currentWorker = BoundedPostgresTestWorker("PostgreSQL current-address contender") {
                second.create(NewUser(Username("postgres-pending-contender"), pending)).single()
            }
            backendPids.arm()
            runCleanupProtectedPostgresContention(
                workers = listOf(pendingWorker, currentWorker),
                cleanupActions = listOf(releaseFirst::countDown),
            ) {
                pendingWorker.start()
                assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
                currentWorker.start()
                assertPostgresContenderBlockedOnUsersWriteLock(schemaUrl, backendPids)
                assertEquals(1, secondLockAcquired.count)
            }
            assertEquals(pending, checkNotNull(pendingWorker.result().getOrThrow()).pendingEmail)
            assertIs<DuplicateUserFieldException>(currentWorker.result().exceptionOrNull())
            assertRawAddressHasOneClaim(schemaUrl, pending)
            assertNoRawSharedAddress(schemaUrl)
        }
    }

    /** Unrelated PostgreSQL claims complete after durable singleton-row serialization. */
    @Test
    fun independentRepositoriesCompleteUnrelatedClaimsAfterSerialization() = runBlockingPostgresTest {
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondLockAcquired = CountDownLatch(1)
        val backendPids = PostgresBackendPids()
        withBoundedPostgresUsersRepos(
            firstAfterWriteLock = {
                firstLockAcquired.countDown()
                check(releaseFirst.await(15, TimeUnit.SECONDS)) { "PostgreSQL unrelated-address worker was not released" }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
            captureBackendPids = backendPids,
        ) { schemaUrl, first, second ->
            val firstAddress = Email("postgres-unrelated-first@example.com")
            val secondAddress = Email("postgres-unrelated-second@example.com")
            val firstWorker = BoundedPostgresTestWorker("PostgreSQL first unrelated-address worker") {
                first.create(NewUser(Username("postgres-unrelated-first"), firstAddress)).single()
            }
            val secondWorker = BoundedPostgresTestWorker("PostgreSQL second unrelated-address worker") {
                second.create(NewUser(Username("postgres-unrelated-second"), secondAddress)).single()
            }
            backendPids.arm()
            runCleanupProtectedPostgresContention(
                workers = listOf(firstWorker, secondWorker),
                cleanupActions = listOf(releaseFirst::countDown),
            ) {
                firstWorker.start()
                assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
                secondWorker.start()
                assertPostgresContenderBlockedOnUsersWriteLock(schemaUrl, backendPids)
                assertEquals(1, secondLockAcquired.count)
            }
            assertEquals(firstAddress, firstWorker.result().getOrThrow().email)
            assertEquals(secondAddress, secondWorker.result().getOrThrow().email)
        }
    }

    /** PostgreSQL lock timeouts remain infrastructure failures rather than lifecycle feedback. */
    @Test
    fun postgresWriterContentionIsNotClassifiedAsDuplicateOrCooldown() = runBlockingPostgresTest {
        withPostgresUsersRepos { schemaUrl, _, _ ->
            val contendedDatabase = Database.connect(
                url = postgresUrlWithLockTimeout(postgresUrlWithDriverBounds(schemaUrl)),
                driver = "org.postgresql.Driver",
                setupConnection = { connection -> configurePostgresTestConnection(connection, lockTimeoutMillis = 100) },
            )
            try {
                val contendedRepo = ExposedUsersRepo(contendedDatabase)
                openBoundedPostgresConnection(schemaUrl).use { connection ->
                    connection.autoCommit = false
                    try {
                        connection.createStatement().use { statement ->
                            statement.queryTimeout = 25
                            statement.executeUpdate("UPDATE users_write_lock SET marker = 0 WHERE id = 1")
                        }
                        val failure = assertFailsWith<ExposedSQLException> {
                            contendedRepo.create(NewUser(Username("postgres-contention"), Email("postgres-contention@example.com")))
                        }
                        val infrastructureFailure: Throwable = failure
                        assertFalse(infrastructureFailure is DuplicateUserFieldException)
                        assertFalse(infrastructureFailure is EmailChangeCooldownException)
                    } finally {
                        connection.rollback()
                    }
                }
            } finally {
                TransactionManager.closeAndUnregister(contendedDatabase)
            }
        }
    }

    /** A PostgreSQL candidate write samples request time only after a verified singleton-row lock wait. */
    @Test
    fun blockedCandidateCreateUsesClockAfterObservedPostgresLockWait() = runBlockingPostgresTest {
        var now = 1_000L
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val contenderLocked = CountDownLatch(1)
        val backendPids = PostgresBackendPids()
        withBoundedPostgresUsersRepos(
            firstNowMillis = { now },
            firstAfterWriteLock = {
                holderLocked.countDown()
                check(releaseHolder.await(15, TimeUnit.SECONDS)) { "PostgreSQL candidate holder was not released" }
            },
            secondNowMillis = { now },
            secondAfterWriteLock = { contenderLocked.countDown() },
            captureBackendPids = backendPids,
        ) { schemaUrl, first, second ->
            val holderWorker = BoundedPostgresTestWorker("PostgreSQL candidate holder") {
                first.create(NewUser(Username("postgres-candidate-holder"), Email("postgres-candidate-holder@example.com"))).single()
            }
            val contenderWorker = BoundedPostgresTestWorker("PostgreSQL candidate contender") {
                second.create(NewUser(Username("postgres-candidate-contender"), Email("postgres-candidate-contender@example.com"))).single()
            }
            backendPids.arm()
            runCleanupProtectedPostgresContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertPostgresContenderBlockedOnUsersWriteLock(schemaUrl, backendPids)
                assertEquals(1, contenderLocked.count)
                now = 2_000L
            }
            val contender = contenderWorker.result().getOrThrow()
            assertEquals(2_000L, first.getEmailProfileFresh(contender.id)?.emailChangeRequestedAt)
        }
    }

    /** Additive PostgreSQL initialization preserves legacy data and survives reopening with lifecycle state intact. */
    @Test
    fun postgresAdditiveMigrationAndReopenPreserveLegacyAndLifecycleState() = runBlockingPostgresTest {
        withPostgresUsersSchema { schemaUrl ->
            openBoundedPostgresConnection(schemaUrl).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE users (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, username TEXT NOT NULL UNIQUE, email TEXT, email_approved BOOLEAN NOT NULL DEFAULT FALSE)")
                    statement.execute("INSERT INTO users (id, username, email, email_approved) VALUES (41, 'postgres-legacy', 'postgres-legacy@example.com', TRUE)")
                }
            }
            val legacyId = UserId(41L)
            val replacement = Email("postgres-legacy-pending@example.com")
            val deadlineAddress = Email("postgres-deadline@example.com")
            val firstDatabase = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
            try {
                val repo = ExposedUsersRepo(firstDatabase, nowMillis = { 1_000L })
                val legacy = checkNotNull(repo.getById(legacyId))
                assertEquals(Email("postgres-legacy@example.com"), legacy.email)
                assertTrue(legacy.emailApproved)
                assertNull(legacy.pendingEmail)
                assertNull(legacy.emailChangeAllowedAt)
                assertNull(repo.getEmailProfileFresh(legacyId)?.emailChangeRequestedAt)
                assertLegacyColumnsAndSingleton(schemaUrl)
                val pending = checkNotNull(repo.setEmail(legacyId, replacement))
                assertEquals(replacement, pending.pendingEmail)
                assertEquals(1_000L, repo.getEmailProfileFresh(legacyId)?.emailChangeRequestedAt)
                val deadlineUser = repo.create(NewUser(Username("postgres-deadline"), deadlineAddress)).single()
                assertEquals(1_000L, repo.getEmailProfileFresh(deadlineUser.id)?.emailChangeRequestedAt)
                assertEquals(1_500L, checkNotNull(repo.approveEmail(deadlineUser.id, deadlineAddress, cooldownMillis = 500)).emailChangeAllowedAt)
                assertNull(repo.getEmailProfileFresh(deadlineUser.id)?.emailChangeRequestedAt)
                assertLegacyColumnsAndSingleton(schemaUrl, replacement)
            } finally {
                TransactionManager.closeAndUnregister(firstDatabase)
            }

            val reopenedDatabase = Database.connect(url = schemaUrl, driver = "org.postgresql.Driver")
            try {
                val reopened = ExposedUsersRepo(reopenedDatabase)
                val legacy = checkNotNull(reopened.getById(legacyId))
                assertEquals(Email("postgres-legacy@example.com"), legacy.email)
                assertEquals(replacement, legacy.pendingEmail)
                assertNull(legacy.emailChangeAllowedAt)
                assertEquals(1_000L, reopened.getEmailProfileFresh(legacyId)?.emailChangeRequestedAt)
                val deadlineUser = checkNotNull(reopened.getUserByUsername(Username("postgres-deadline")))
                assertEquals(1_500L, deadlineUser.emailChangeAllowedAt)
                assertNull(reopened.getEmailProfileFresh(deadlineUser.id)?.emailChangeRequestedAt)
                assertLegacyColumnsAndSingleton(schemaUrl, replacement)
            } finally {
                TransactionManager.closeAndUnregister(reopenedDatabase)
            }
        }
    }

    /** Executes a blocking test body without virtual-time scheduling. */
    private fun runBlockingPostgresTest(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }

    /** Builds two independently bounded PostgreSQL repositories inside one fixture-owned schema. */
    private suspend fun withBoundedPostgresUsersRepos(
        firstNowMillis: () -> Long = System::currentTimeMillis,
        firstAfterWriteLock: (() -> Unit)? = null,
        secondNowMillis: () -> Long = System::currentTimeMillis,
        secondAfterWriteLock: (() -> Unit)? = null,
        captureBackendPids: PostgresBackendPids? = null,
        block: suspend (String, ExposedUsersRepo, ExposedUsersRepo) -> Unit,
    ) = withPostgresUsersSchema { schemaUrl ->
        val boundedSchemaUrl = postgresUrlWithDriverBounds(schemaUrl)
        val firstDatabase = Database.connect(
            url = boundedSchemaUrl,
            driver = "org.postgresql.Driver",
            setupConnection = { connection ->
                configurePostgresTestConnection(connection)
                captureBackendPids?.captureFirst(connection)
            },
        )
        val secondDatabase = Database.connect(
            url = boundedSchemaUrl,
            driver = "org.postgresql.Driver",
            setupConnection = { connection ->
                configurePostgresTestConnection(connection)
                captureBackendPids?.captureSecond(connection)
            },
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

    /** Opens an observer or direct-test connection with finite PostgreSQL driver and JDBC bounds. */
    private fun openBoundedPostgresConnection(schemaUrl: String): java.sql.Connection =
        DriverManager.getConnection(postgresUrlWithDriverBounds(schemaUrl)).also(::configurePostgresTestConnection)

    /** Configures finite JDBC network, server statement, and server lock bounds for one test connection. */
    private fun configurePostgresTestConnection(
        connection: java.sql.Connection,
        lockTimeoutMillis: Int = 25_000,
    ) {
        connection.setNetworkTimeout(postgresNetworkTimeoutExecutor, 25_000)
        connection.createStatement().use { statement ->
            statement.queryTimeout = 25
            statement.execute("SET statement_timeout = '25000ms'")
            statement.execute("SET lock_timeout = '${lockTimeoutMillis}ms'")
        }
    }

    /** Adds finite PostgreSQL connect and socket bounds without replacing fixture-owned schema parameters. */
    private fun postgresUrlWithDriverBounds(schemaUrl: String): String =
        "$schemaUrl${urlParameterSeparator(schemaUrl)}connectTimeout=5&loginTimeout=5&socketTimeout=25&tcpKeepAlive=true"

    /** Selects the URL separator required by an existing PostgreSQL JDBC parameter list. */
    private fun urlParameterSeparator(url: String): Char = if ('?' in url) '&' else '?'

    /** Asserts exactly one raw current-or-pending claim for [address] across fixture rows. */
    private fun assertRawAddressHasOneClaim(schemaUrl: String, address: Email) {
        openBoundedPostgresConnection(schemaUrl).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ? OR pending_email = ?").use { statement ->
                statement.setString(1, address.string)
                statement.setString(2, address.string)
                statement.executeQuery().use { result ->
                    assertTrue(result.next())
                    assertEquals(1, result.getInt(1))
                }
            }
        }
    }

    /** Asserts no two fixture rows share an address across raw current and pending columns. */
    private fun assertNoRawSharedAddress(schemaUrl: String) {
        openBoundedPostgresConnection(schemaUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT COUNT(*) FROM users first_user JOIN users second_user ON first_user.id < second_user.id " +
                        "WHERE first_user.email = second_user.email OR first_user.email = second_user.pending_email " +
                        "OR first_user.pending_email = second_user.email OR first_user.pending_email = second_user.pending_email",
                ).use { result ->
                    assertTrue(result.next())
                    assertEquals(0, result.getInt(1))
                }
            }
        }
    }

    /** Verifies new nullable columns and singleton lock row without modifying legacy values. */
    private fun assertLegacyColumnsAndSingleton(schemaUrl: String, expectedPending: Email? = null) {
        openBoundedPostgresConnection(schemaUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT pending_email, email_change_requested_at, email_change_allowed_at FROM users WHERE id = 41").use { result ->
                    assertTrue(result.next())
                    assertEquals(expectedPending?.string, result.getString("pending_email"))
                    if (expectedPending == null) assertNull(result.getObject("email_change_requested_at"))
                    assertNull(result.getObject("email_change_allowed_at"))
                }
                statement.executeQuery("SELECT COUNT(*) FROM users_write_lock WHERE id = 1 AND marker = 0").use { result ->
                    assertTrue(result.next())
                    assertEquals(1, result.getInt(1))
                }
            }
        }
    }

    /** Adds a short PostgreSQL session lock timeout to a fixture-owned schema URL. */
    private fun postgresUrlWithLockTimeout(schemaUrl: String): String =
        "$schemaUrl${urlParameterSeparator(schemaUrl)}options=-c%20lock_timeout=100ms"

    /** Observes an active contender blocked by the holder on the physical singleton-row UPDATE. */
    private fun assertPostgresContenderBlockedOnUsersWriteLock(schemaUrl: String, backendPids: PostgresBackendPids) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var lastObservation = "contender PID was not captured"
        while (System.nanoTime() < deadline) {
            val holderPid = backendPids.firstPid()
            val contenderPid = backendPids.secondPid()
            if (holderPid > 0 && contenderPid > 0) {
                openBoundedPostgresConnection(schemaUrl).use { observer ->
                    observer.prepareStatement(
                        "SELECT state, wait_event_type, query, pg_blocking_pids(pid) FROM pg_stat_activity WHERE pid = ?",
                    ).use { statement ->
                        statement.queryTimeout = 5
                        statement.setInt(1, contenderPid)
                        statement.executeQuery().use { row ->
                            if (row.next()) {
                                val state = row.getString("state")
                                val waitEventType = row.getString("wait_event_type")
                                val query = row.getString("query") ?: ""
                                val blockers = row.getArray("pg_blocking_pids")?.array as? Array<*> ?: emptyArray<Any>()
                                lastObservation = "state=$state wait_event_type=$waitEventType query=$query blockers=${blockers.contentToString()}"
                                if (
                                    state == "active" &&
                                    waitEventType == "Lock" &&
                                    query.contains("users_write_lock", ignoreCase = true) &&
                                    query.contains("UPDATE", ignoreCase = true) &&
                                    blockers.any { it == holderPid || it == holderPid.toLong() }
                                ) return
                            }
                        }
                    }
                }
            }
            Thread.sleep(25)
        }
        throw AssertionError("PostgreSQL contender never reached a holder-blocked users_write_lock UPDATE: $lastObservation")
    }

    /** Supplies daemon timeout tasks to JDBC so a failed driver timeout cannot retain a non-daemon executor thread. */
    private val postgresNetworkTimeoutExecutor = Executor { command ->
        Thread(command, "PostgreSQL test network timeout").apply { isDaemon = true }.start()
    }

    /** Runs one PostgreSQL lifecycle operation on a daemon thread and stores its terminal result. */
    private inner class BoundedPostgresTestWorker<T>(
        /** Human-readable identity included in timeout failures. */
        private val description: String,
        /** Suspending lifecycle operation whose value or failure must be asserted by the test. */
        private val action: suspend () -> T,
    ) {
        /** Records whether [start] successfully handed the worker to the JVM. */
        private val started = AtomicBoolean(false)

        /** Signals completion independently from the worker's finite join operation. */
        private val completed = CountDownLatch(1)

        /** Retains the worker value or failure without rethrowing on the worker thread. */
        private val outcome = AtomicReference<Result<T>?>(null)

        /** Daemon containment prevents a failed test from leaving a non-daemon PostgreSQL worker alive. */
        private val thread = Thread(
            {
                try {
                    outcome.set(runCatching { kotlinx.coroutines.runBlocking { action() } })
                } finally {
                    completed.countDown()
                }
            },
            description,
        ).apply { isDaemon = true }

        /** Starts the worker once while the caller's cleanup guard is already active. */
        fun start() {
            check(started.compareAndSet(false, true)) { "$description was started more than once" }
            thread.start()
        }

        /** Returns the captured result after cleanup has proven that the worker terminated. */
        fun result(): Result<T> = checkNotNull(outcome.get()) { "$description produced no terminal outcome" }

        /** Finite cleanup that continues remaining releases and joins after a prior cleanup failure. */
        fun awaitAndJoin(cleanupFailures: MutableList<Throwable>) {
            if (!started.get()) return
            collectPostgresCleanupFailure(cleanupFailures) {
                assertTrue(completed.await(15, TimeUnit.SECONDS), "$description did not signal completion")
            }
            collectPostgresCleanupFailure(cleanupFailures) {
                thread.join(TimeUnit.SECONDS.toMillis(15))
            }
            if (thread.isAlive) {
                collectPostgresCleanupFailure(cleanupFailures) {
                    thread.interrupt()
                    thread.join(TimeUnit.SECONDS.toMillis(15))
                }
            }
            collectPostgresCleanupFailure(cleanupFailures) {
                assertFalse(thread.isAlive, "$description outlived bounded cleanup")
                checkNotNull(outcome.get()) { "$description completed without a recorded outcome" }
            }
        }
    }

    /** Runs a PostgreSQL contention body with unconditional release and aggregate worker cleanup. */
    private inline fun <T> runCleanupProtectedPostgresContention(
        workers: List<BoundedPostgresTestWorker<*>>,
        cleanupActions: List<() -> Unit>,
        block: () -> T,
    ): T {
        var primaryFailure: Throwable? = null
        return try {
            block()
        } catch (error: Throwable) {
            primaryFailure = error
            throw error
        } finally {
            val cleanupFailures = mutableListOf<Throwable>()
            cleanupActions.forEach { action -> collectPostgresCleanupFailure(cleanupFailures, action) }
            workers.forEach { worker -> worker.awaitAndJoin(cleanupFailures) }
            if (cleanupFailures.isNotEmpty()) {
                val cleanupFailure = AssertionError("PostgreSQL contention worker cleanup failed")
                cleanupFailures.forEach(cleanupFailure::addSuppressed)
                primaryFailure?.addSuppressed(cleanupFailure)
                if (primaryFailure == null) throw cleanupFailure
            }
        }
    }

    /** Records one cleanup failure while allowing all other cleanup actions to continue. */
    private fun collectPostgresCleanupFailure(cleanupFailures: MutableList<Throwable>, action: () -> Unit) {
        runCatching(action).exceptionOrNull()?.let(cleanupFailures::add)
    }
}
