package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.DriverManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies approval migration and duplicate-field translation through the real Exposed/Xerial SQLite stack. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExposedUsersRepoSqliteTest {
    /** A duplicate username maps to the repository's existing duplicate-field contract. */
    @Test
    fun createDuplicateUsernameMapsToDuplicateUserFieldException() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val seeded = repo.create(
                NewUser(Username("alice"), Email("alice@example.com"))
            ).single()
            val before = repo.getAll()

            val failure = assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("alice"), Email("other@example.com")))
            }

            assertSqliteUniqueCause(failure)
            assertEquals(mapOf(seeded.id to seeded), before)
            assertEquals(before, repo.getAll())
        }
    }

    /** A duplicate non-null email maps to the repository's existing duplicate-field contract. */
    @Test
    fun createDuplicateEmailMapsToDuplicateUserFieldException() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val seeded = repo.create(
                NewUser(Username("alice"), Email("shared@example.com"))
            ).single()
            val before = repo.getAll()

            val failure = assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("bob"), Email("shared@example.com")))
            }

            assertNull(failure.cause)
            assertEquals(mapOf(seeded.id to seeded), before)
            assertEquals(before, repo.getAll())
        }
    }

    /** A username collision during update is translated without mutating either stored row. */
    @Test
    fun updateDuplicateUsernameMapsToDuplicateUserFieldException() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val first = repo.create(
                NewUser(Username("alice"), Email("alice@example.com"))
            ).single()
            val second = repo.create(
                NewUser(Username("bob"), Email("bob@example.com"))
            ).single()
            val before = repo.getAll()

            val failure = assertFailsWith<DuplicateUserFieldException> {
                repo.update(second.id, NewUser(first.username, second.email))
            }

            assertSqliteUniqueCause(failure)
            assertEquals(before, repo.getAll())
        }
    }

    /** A non-null email collision during update is translated without mutating stored fields. */
    @Test
    fun updateDuplicateEmailMapsToDuplicateUserFieldException() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val first = repo.create(
                NewUser(Username("alice"), Email("alice@example.com"))
            ).single()
            val second = repo.create(
                NewUser(Username("bob"), Email("bob@example.com"))
            ).single()
            val before = repo.getAll()

            val failure = assertFailsWith<DuplicateUserFieldException> {
                repo.update(second.id, NewUser(second.username, first.email))
            }

            assertNull(failure.cause)
            assertEquals(before, repo.getAll())
        }
    }

    /** SQLite permits multiple distinct users to retain null email values. */
    @Test
    fun multipleNullEmailsRemainValid() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val created = repo.create(
                listOf(
                    NewUser(Username("alice")),
                    NewUser(Username("bob")),
                )
            )

            assertEquals(2, created.size)
            assertTrue(created.all { it.email == null })
            assertEquals(created.associateBy { it.id }, repo.getAll())
        }
    }

    /** Approval retains the approved address while a replacement remains pending, then clears explicitly. */
    @Test
    fun emailApprovalTracksOnlyTheCurrentStoredAddress() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val originalEmail = Email("alice@example.com")
            val changedEmail = Email("alice+changed@example.com")
            val created = repo.create(NewUser(Username("alice"), originalEmail)).single()

            assertFalse(created.emailApproved)
            assertNull(repo.approveEmail(created.id, changedEmail))

            val approved = checkNotNull(repo.approveEmail(created.id, originalEmail))
            assertTrue(approved.emailApproved)

            val renamed = checkNotNull(repo.update(created.id, NewUser(Username("alice-renamed"), originalEmail)))
            assertTrue(renamed.emailApproved)

            val changed = checkNotNull(repo.update(created.id, NewUser(renamed.username, changedEmail)))
            assertEquals(originalEmail, changed.email)
            assertTrue(changed.emailApproved)
            assertEquals(changedEmail, changed.pendingEmail)
            assertNull(repo.approveEmail(created.id, originalEmail))

            val cleared = checkNotNull(repo.update(created.id, NewUser(changed.username, null)))
            assertEquals(null, cleared.email)
            assertNull(cleared.pendingEmail)
            assertFalse(cleared.emailApproved)
        }
    }

    /** A pre-approval schema retains an existing address and identity while defaulting approval to false. */
    @Test
    fun legacySchemaAddsFalseApprovalWithoutChangingUsers() = runTest {
        val databaseFile = Files.createTempFile("wishlist-users-legacy", ".sqlite")
        val url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        try {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT)")
                    statement.execute("INSERT INTO users (id, username, email) VALUES (41, 'legacy-user', 'legacy@example.com')")
                }
            }
            val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val repo = ExposedUsersRepo(database)
                val legacy = checkNotNull(repo.getUserByUsername(Username("legacy-user")))

                assertEquals(UserId(41L), legacy.id)
                assertEquals(Username("legacy-user"), legacy.username)
                assertEquals(Email("legacy@example.com"), legacy.email)
                assertFalse(legacy.emailApproved)
            } finally {
                TransactionManager.closeAndUnregister(database)
            }
        } finally {
            Files.deleteIfExists(databaseFile)
        }
    }

    /** A successful conditional approval is durable across a fresh database and repository instance. */
    @Test
    fun approvalSurvivesDatabaseReopen() = runTest {
        val databaseFile = Files.createTempFile("wishlist-users-approval", ".sqlite")
        val url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        val email = Email("durable@example.com")
        try {
            val firstDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            val userId = try {
                val repo = ExposedUsersRepo(firstDatabase)
                val created = repo.create(NewUser(Username("durable"), email)).single()
                assertTrue(checkNotNull(repo.approveEmail(created.id, email)).emailApproved)
                created.id
            } finally {
                TransactionManager.closeAndUnregister(firstDatabase)
            }

            val reopenedDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val reopened = checkNotNull(ExposedUsersRepo(reopenedDatabase).getById(userId))
                assertEquals(email, reopened.email)
                assertTrue(reopened.emailApproved)
            } finally {
                TransactionManager.closeAndUnregister(reopenedDatabase)
            }
        } finally {
            Files.deleteIfExists(databaseFile)
        }
    }

    /** Bulk updates use the same pending replacement lifecycle as single-row writes. */
    @Test
    fun bulkUpdatesRetainApprovalAndStoreReplacementAsPending() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val original = Email("bulk@example.com")
            val replacement = Email("bulk-replacement@example.com")
            val created = repo.create(NewUser(Username("bulk"), original)).single()
            checkNotNull(repo.approveEmail(created.id, original))

            val sameAddress = repo.update(listOf(created.id to NewUser(Username("bulk-renamed"), original))).single()
            assertTrue(sameAddress.emailApproved)

            val changedAddress = repo.update(listOf(created.id to NewUser(sameAddress.username, replacement))).single()
            assertEquals(original, changedAddress.email)
            assertEquals(replacement, changedAddress.pendingEmail)
            assertTrue(changedAddress.emailApproved)

            val cleared = repo.update(listOf(created.id to NewUser(changedAddress.username, null))).single()
            assertNull(cleared.email)
            assertFalse(cleared.emailApproved)
        }
    }

    /** A failed bulk collision leaves both stored rows unchanged under the direct atomic contract. */
    @Test
    fun failedBulkUpdateLeavesRowsUnchanged() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val first = repo.create(NewUser(Username("first"), Email("first@example.com"))).single()
            val second = repo.create(NewUser(Username("second"), Email("second@example.com"))).single()
            val before = repo.getAll()

            assertFailsWith<DuplicateUserFieldException> {
                repo.update(listOf(second.id to NewUser(first.username, second.email)))
            }

            assertEquals(before, repo.getAll())
        }
    }

    /** A later cross-slot conflict rolls back earlier batch mutations and emits no speculative update event. */
    @Test
    fun failedBulkUpdateRollsBackEarlierMutationAndEvents() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val occupied = repo.create(NewUser(Username("occupied"), Email("occupied@example.com"))).single()
            val first = repo.create(NewUser(Username("first"), Email("first@example.com"))).single()
            val second = repo.create(NewUser(Username("second"), Email("second@example.com"))).single()
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertFailsWith<DuplicateUserFieldException> {
                    repo.update(
                        listOf(
                            first.id to NewUser(Username("first-renamed"), first.email),
                            second.id to NewUser(second.username, occupied.email),
                        )
                    )
                }
                advanceUntilIdle()
                assertEquals(first, repo.getById(first.id))
                assertEquals(second, repo.getById(second.id))
                assertEquals(emptyList(), events)
            } finally {
                collector.cancel()
            }
        }
    }

    /** A locked injected clock rejects before duplicate disclosure and approval replay preserves the issued deadline. */
    @Test
    fun cooldownIsExactAndApprovalReplaySafe() = runTest {
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val initial = Email("cooldown-initial@example.com")
            val replacement = Email("cooldown-replacement@example.com")
            val occupied = Email("cooldown-occupied@example.com")
            val user = repo.create(NewUser(Username("cooldown"), initial)).single()
            repo.create(NewUser(Username("occupied"), occupied))
            val approved = checkNotNull(repo.approveEmail(user.id, initial, cooldownMillis = 500))
            assertEquals(1_500L, approved.emailChangeAllowedAt)

            val blocked = assertFailsWith<EmailChangeCooldownException> {
                repo.setEmail(user.id, occupied)
            }
            assertEquals(1_500L, blocked.emailChangeAllowedAt)
            assertEquals(approved, repo.getById(user.id))

            now = 1_500L
            val pending = checkNotNull(repo.setEmail(user.id, replacement))
            assertEquals(replacement, pending.pendingEmail)
            val promoted = checkNotNull(repo.approveEmail(user.id, replacement, cooldownMillis = 500))
            assertEquals(2_000L, promoted.emailChangeAllowedAt)

            now = 9_000L
            assertEquals(promoted, repo.approveEmail(user.id, replacement, cooldownMillis = 999))
        }
    }

    /** Conditional approval emits only after the address predicate succeeds. */
    @Test
    fun conditionalApprovalEmitsOnlySuccessfulUpdates() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val email = Email("events@example.com")
            val created = repo.create(NewUser(Username("events"), email)).single()
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertNull(repo.approveEmail(created.id, Email("stale@example.com")))
                advanceUntilIdle()
                assertEquals(emptyList(), events)

                val approved = checkNotNull(repo.approveEmail(created.id, email))
                advanceUntilIdle()
                assertEquals(listOf(approved), events)
            } finally {
                collector.cancel()
            }
        }
    }

    /** A real non-constraint SQLite failure preserves the original Exposed exception type. */
    @Test
    fun nonUniqueDatabaseFailureRemainsExposedSQLException() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            transaction(db = repo.database) {
                SchemaUtils.drop(repo)
            }

            val failure = assertFailsWith<ExposedSQLException> {
                repo.create(NewUser(Username("alice"), Email("alice@example.com")))
            }

            val sqlite = assertIs<SQLiteException>(failure.cause)
            assertEquals(SQLiteErrorCode.SQLITE_ERROR, sqlite.resultCode)
        }
    }

    /** Verifies the retained Exposed/Xerial cause chain and exact UNIQUE extended result code. */
    private fun assertSqliteUniqueCause(failure: DuplicateUserFieldException) {
        val exposed = assertIs<ExposedSQLException>(failure.cause)
        val sqlite = assertIs<SQLiteException>(exposed.cause)
        assertNull(sqlite.sqlState)
        assertEquals(19, sqlite.errorCode)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, sqlite.resultCode)
    }

}
