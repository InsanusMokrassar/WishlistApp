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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
                DriverManager.getConnection(schemaUrl).use { connection ->
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
        withPostgresUsersRepos(
            firstAfterWriteLock = {
                firstLockAcquired.countDown()
                releaseFirst.await()
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
        ) { schemaUrl, first, second ->
            val shared = Email("postgres-concurrent-current@example.com")
            var firstWrite: Result<RegisteredUser>? = null
            var secondWrite: Result<RegisteredUser>? = null
            val firstDone = CountDownLatch(1)
            val secondDone = CountDownLatch(1)
            val firstThread = Thread {
                firstWrite = runCatching { kotlinx.coroutines.runBlocking { first.create(NewUser(Username("postgres-current-first"), shared)).single() } }
                firstDone.countDown()
            }
            val secondThread = Thread {
                secondWrite = runCatching { kotlinx.coroutines.runBlocking { second.create(NewUser(Username("postgres-current-second"), shared)).single() } }
                secondDone.countDown()
            }
            firstThread.start()
            assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
            secondThread.start()
            try {
                assertEquals(1, secondLockAcquired.count)
            } finally {
                releaseFirst.countDown()
            }
            assertTrue(firstDone.await(10, TimeUnit.SECONDS))
            assertTrue(secondDone.await(10, TimeUnit.SECONDS))
            assertEquals(shared, checkNotNull(firstWrite).getOrThrow().email)
            assertIs<DuplicateUserFieldException>(checkNotNull(secondWrite).exceptionOrNull())
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
        withPostgresUsersRepos(
            firstAfterWriteLock = {
                if (blockFirstWriter.get()) {
                    firstLockAcquired.countDown()
                    releaseFirst.await()
                }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
        ) { schemaUrl, first, second ->
            val current = Email("postgres-pending-owner@example.com")
            val pending = Email("postgres-pending-contended@example.com")
            val owner = first.create(NewUser(Username("postgres-pending-owner"), current)).single()
            checkNotNull(first.approveEmail(owner.id, current))
            blockFirstWriter.set(true)

            var pendingWrite: Result<RegisteredUser?>? = null
            var currentWrite: Result<RegisteredUser>? = null
            val pendingDone = CountDownLatch(1)
            val currentDone = CountDownLatch(1)
            val pendingThread = Thread {
                pendingWrite = runCatching { kotlinx.coroutines.runBlocking { first.setEmail(owner.id, pending) } }
                pendingDone.countDown()
            }
            val currentThread = Thread {
                currentWrite = runCatching { kotlinx.coroutines.runBlocking { second.create(NewUser(Username("postgres-pending-contender"), pending)).single() } }
                currentDone.countDown()
            }
            pendingThread.start()
            assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
            currentThread.start()
            try {
                assertEquals(1, secondLockAcquired.count)
            } finally {
                releaseFirst.countDown()
            }
            assertTrue(pendingDone.await(10, TimeUnit.SECONDS))
            assertTrue(currentDone.await(10, TimeUnit.SECONDS))
            assertEquals(pending, checkNotNull(checkNotNull(pendingWrite).getOrThrow()).pendingEmail)
            assertIs<DuplicateUserFieldException>(checkNotNull(currentWrite).exceptionOrNull())
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
        withPostgresUsersRepos(
            firstAfterWriteLock = {
                firstLockAcquired.countDown()
                releaseFirst.await()
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
        ) { _, first, second ->
            val firstAddress = Email("postgres-unrelated-first@example.com")
            val secondAddress = Email("postgres-unrelated-second@example.com")
            var firstWrite: Result<RegisteredUser>? = null
            var secondWrite: Result<RegisteredUser>? = null
            val firstDone = CountDownLatch(1)
            val secondDone = CountDownLatch(1)
            Thread {
                firstWrite = runCatching { kotlinx.coroutines.runBlocking { first.create(NewUser(Username("postgres-unrelated-first"), firstAddress)).single() } }
                firstDone.countDown()
            }.start()
            assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
            Thread {
                secondWrite = runCatching { kotlinx.coroutines.runBlocking { second.create(NewUser(Username("postgres-unrelated-second"), secondAddress)).single() } }
                secondDone.countDown()
            }.start()
            try {
                assertEquals(1, secondLockAcquired.count)
            } finally {
                releaseFirst.countDown()
            }
            assertTrue(firstDone.await(10, TimeUnit.SECONDS))
            assertTrue(secondDone.await(10, TimeUnit.SECONDS))
            assertEquals(firstAddress, checkNotNull(firstWrite).getOrThrow().email)
            assertEquals(secondAddress, checkNotNull(secondWrite).getOrThrow().email)
        }
    }

    /** PostgreSQL lock timeouts remain infrastructure failures rather than lifecycle feedback. */
    @Test
    fun postgresWriterContentionIsNotClassifiedAsDuplicateOrCooldown() = runBlockingPostgresTest {
        withPostgresUsersRepos { schemaUrl, _, _ ->
            val contendedDatabase = Database.connect(url = postgresUrlWithLockTimeout(schemaUrl), driver = "org.postgresql.Driver")
            try {
                val contendedRepo = ExposedUsersRepo(contendedDatabase)
                DriverManager.getConnection(schemaUrl).use { connection ->
                    connection.autoCommit = false
                    try {
                        connection.createStatement().use { statement ->
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

    /** Additive PostgreSQL initialization preserves legacy data and survives reopening with lifecycle state intact. */
    @Test
    fun postgresAdditiveMigrationAndReopenPreserveLegacyAndLifecycleState() = runBlockingPostgresTest {
        withPostgresUsersSchema { schemaUrl ->
            DriverManager.getConnection(schemaUrl).use { connection ->
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
                assertLegacyColumnsAndSingleton(schemaUrl)
                val pending = checkNotNull(repo.setEmail(legacyId, replacement))
                assertEquals(replacement, pending.pendingEmail)
                val deadlineUser = repo.create(NewUser(Username("postgres-deadline"), deadlineAddress)).single()
                assertEquals(1_500L, checkNotNull(repo.approveEmail(deadlineUser.id, deadlineAddress, cooldownMillis = 500)).emailChangeAllowedAt)
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
                val deadlineUser = checkNotNull(reopened.getUserByUsername(Username("postgres-deadline")))
                assertEquals(1_500L, deadlineUser.emailChangeAllowedAt)
                assertLegacyColumnsAndSingleton(schemaUrl, replacement)
            } finally {
                TransactionManager.closeAndUnregister(reopenedDatabase)
            }
        }
    }

    /** Executes a blocking test body without virtual-time scheduling. */
    private fun runBlockingPostgresTest(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }

    /** Asserts exactly one raw current-or-pending claim for [address] across fixture rows. */
    private fun assertRawAddressHasOneClaim(schemaUrl: String, address: Email) {
        DriverManager.getConnection(schemaUrl).use { connection ->
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
        DriverManager.getConnection(schemaUrl).use { connection ->
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
        DriverManager.getConnection(schemaUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT pending_email, email_change_allowed_at FROM users WHERE id = 41").use { result ->
                    assertTrue(result.next())
                    assertEquals(expectedPending?.string, result.getString("pending_email"))
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
    private fun postgresUrlWithLockTimeout(schemaUrl: String): String = "$schemaUrl&options=-c%20lock_timeout=100ms"
}
