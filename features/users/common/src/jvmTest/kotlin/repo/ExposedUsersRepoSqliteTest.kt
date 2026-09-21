package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
            assertEquals(changedEmail, repo.getEmailProfileFresh(created.id)?.pendingEmail)
            assertNull(repo.approveEmail(created.id, originalEmail))

            val cleared = checkNotNull(repo.update(created.id, NewUser(changed.username, null)))
            assertEquals(null, cleared.email)
            assertFalse(cleared.emailApproved)
            assertEquals(EmailProfile(userId = created.id.long), repo.getEmailProfileFresh(created.id))
        }
    }

    /** A one-row fresh projection retains complete email lifecycle state outside the cache. */
    @Test
    fun freshEmailProfileReturnsCompleteSingleRow() = runTest {
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val current = Email("profile-current@example.com")
            val pending = Email("profile-pending@example.com")
            val created = repo.create(NewUser(Username("profile"), current)).single()
            now = 1_200L
            checkNotNull(repo.approveEmail(created.id, current, cooldownMillis = 300L))
            now = 1_500L
            checkNotNull(repo.setEmail(created.id, pending))

            assertEquals(
                EmailProfile(
                    userId = created.id.long,
                    email = current,
                    emailApproved = true,
                    pendingEmail = pending,
                    emailChangeRequestedAt = 1_500L,
                    emailChangeAllowedAt = 1_500L,
                ),
                repo.getEmailProfileFresh(created.id),
            )
            val empty = repo.create(NewUser(Username("empty"))).single()
            assertEquals(EmailProfile(userId = empty.id.long), repo.getEmailProfileFresh(empty.id))
            assertNull(repo.getEmailProfileFresh(UserId(1_000L)))
        }
    }

    /** Request time changes only for accepted candidate writes and clears with approval or explicit clear. */
    @Test
    fun requestedTimeTracksCandidateLifecycleWithoutBackfill() = runTest {
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val first = Email("requested-first@example.com")
            val replacement = Email("requested-replacement@example.com")
            val secondReplacement = Email("requested-second@example.com")
            val created = repo.create(NewUser(Username("requested"), first)).single()
            assertEquals(1_000L, repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)

            now = 1_100L
            checkNotNull(repo.updateUsername(created.id, Username("requested-renamed")))
            checkNotNull(repo.setEmail(created.id, first))
            assertEquals(1_000L, repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)

            now = 1_200L
            checkNotNull(repo.approveEmail(created.id, first, cooldownMillis = 0L))
            assertNull(repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)

            now = 1_300L
            checkNotNull(repo.setEmail(created.id, replacement))
            assertEquals(1_300L, repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)
            now = 1_400L
            checkNotNull(repo.setEmail(created.id, replacement))
            assertEquals(1_300L, repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)
            checkNotNull(repo.setEmail(created.id, secondReplacement))
            assertEquals(1_400L, repo.getEmailProfileFresh(created.id)?.emailChangeRequestedAt)

            checkNotNull(repo.setEmail(created.id, null))
            assertEquals(EmailProfile(userId = created.id.long), repo.getEmailProfileFresh(created.id))
        }
    }

    /** A clear treats an otherwise invisible raw request-time residue as committed lifecycle state. */
    @Test
    fun clearRemovesRequestOnlyResidueAndPublishesOneEvent() = runTest {
        val databaseFile = Files.createTempFile("wishlist-users-request-residue", ".sqlite")
        val url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
        try {
            val repo = ExposedUsersRepo(database, nowMillis = { 1_000L })
            val created = repo.create(NewUser(Username("request-residue"))).single()
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate("UPDATE users SET email_change_requested_at = 500 WHERE id = ${created.id.long}")
                }
            }
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertEquals(created, repo.setEmail(created.id, null))
                advanceUntilIdle()
                assertEquals(listOf(created), events)
                assertEquals(EmailProfile(userId = created.id.long), repo.getEmailProfileFresh(created.id))
                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT email_change_requested_at FROM users WHERE id = ${created.id.long}").use { row ->
                            assertTrue(row.next())
                            assertNull(row.getObject("email_change_requested_at"))
                        }
                    }
                }
            } finally {
                collector.cancel()
            }
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                Files.deleteIfExists(databaseFile)
            }
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
                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT pending_email, email_change_requested_at, email_change_allowed_at FROM users WHERE id = 41").use { result ->
                            assertTrue(result.next())
                            assertNull(result.getString("pending_email"))
                            assertNull(result.getObject("email_change_requested_at"))
                            assertNull(result.getObject("email_change_allowed_at"))
                        }
                        statement.executeQuery("SELECT COUNT(*) FROM users_write_lock WHERE id = 1 AND marker = 0").use { result ->
                            assertTrue(result.next())
                            assertEquals(1, result.getInt(1))
                        }
                    }
                }
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
            var now = 1_000L
            val firstDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            val userId = try {
                val repo = ExposedUsersRepo(firstDatabase, nowMillis = { now })
                val created = repo.create(NewUser(Username("durable"), email)).single()
                val approved = checkNotNull(repo.approveEmail(created.id, email, cooldownMillis = 500))
                now = checkNotNull(repo.getEmailProfileFresh(approved.id)?.emailChangeAllowedAt)
                val pending = checkNotNull(repo.setEmail(created.id, Email("durable-pending@example.com")))
                assertEquals(Email("durable-pending@example.com"), repo.getEmailProfileFresh(pending.id)?.pendingEmail)
                created.id
            } finally {
                TransactionManager.closeAndUnregister(firstDatabase)
            }

            val reopenedDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val reopened = checkNotNull(ExposedUsersRepo(reopenedDatabase).getById(userId))
                val profile = checkNotNull(ExposedUsersRepo(reopenedDatabase).getEmailProfileFresh(userId))
                assertEquals(email, reopened.email)
                assertTrue(reopened.emailApproved)
                assertEquals(Email("durable-pending@example.com"), profile.pendingEmail)
                assertEquals(1_500L, profile.emailChangeAllowedAt)
                assertEquals(1_500L, profile.emailChangeRequestedAt)
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
            assertTrue(changedAddress.emailApproved)
            assertEquals(replacement, repo.getEmailProfileFresh(created.id)?.pendingEmail)

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
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val occupied = repo.create(NewUser(Username("occupied"), Email("occupied@example.com"))).single()
            val first = repo.create(NewUser(Username("first"), Email("first@example.com"))).single()
            val second = repo.create(NewUser(Username("second"), Email("second@example.com"))).single()
            now = 2_000L
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
                assertEquals(1_000L, repo.getEmailProfileFresh(first.id)?.emailChangeRequestedAt)
                assertEquals(1_000L, repo.getEmailProfileFresh(second.id)?.emailChangeRequestedAt)
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
            assertEquals(1_500L, repo.getEmailProfileFresh(approved.id)?.emailChangeAllowedAt)

            val blocked = assertFailsWith<EmailChangeCooldownException> {
                repo.setEmail(user.id, occupied)
            }
            assertEquals(1_500L, blocked.emailChangeAllowedAt)
            assertEquals(approved, repo.getById(user.id))

            now = 1_500L
            val pending = checkNotNull(repo.setEmail(user.id, replacement))
            assertEquals(replacement, repo.getEmailProfileFresh(pending.id)?.pendingEmail)
            val promoted = checkNotNull(repo.approveEmail(user.id, replacement, cooldownMillis = 500))
            assertEquals(2_000L, repo.getEmailProfileFresh(promoted.id)?.emailChangeAllowedAt)

            now = 9_000L
            assertEquals(promoted, repo.approveEmail(user.id, replacement, cooldownMillis = 999))
        }
    }

    /** Additive migration preserves every old approval state without fabricating pending history or deadlines. */
    @Test
    fun legacyApprovalSchemaPreservesApprovedNullAndMalformedRows() = runTest {
        val databaseFile = Files.createTempFile("wishlist-users-legacy-approval", ".sqlite")
        val url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        try {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT, email_approved BOOLEAN NOT NULL DEFAULT FALSE)")
                    statement.execute("INSERT INTO users (id, username, email, email_approved) VALUES (1, 'approved', 'approved@example.com', 1)")
                    statement.execute("INSERT INTO users (id, username, email, email_approved) VALUES (2, 'unapproved', 'unapproved@example.com', 0)")
                    statement.execute("INSERT INTO users (id, username, email, email_approved) VALUES (3, 'empty', NULL, 1)")
                    statement.execute("INSERT INTO users (id, username, email, email_approved) VALUES (4, 'malformed', 'not-an-email', 1)")
                }
            }
            val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val repo = ExposedUsersRepo(database)
                assertTrue(checkNotNull(repo.getById(UserId(1))).emailApproved)
                assertFalse(checkNotNull(repo.getById(UserId(2))).emailApproved)
                assertFalse(checkNotNull(repo.getById(UserId(3))).emailApproved)
                assertNull(checkNotNull(repo.getById(UserId(4))).email)
                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT COUNT(*) FROM users WHERE pending_email IS NULL AND email_change_requested_at IS NULL AND email_change_allowed_at IS NULL").use { result ->
                            assertTrue(result.next())
                            assertEquals(4, result.getInt(1))
                        }
                        statement.executeQuery("SELECT email, email_approved FROM users WHERE id = 4").use { result ->
                            assertTrue(result.next())
                            assertEquals("not-an-email", result.getString("email"))
                            assertTrue(result.getBoolean("email_approved"))
                        }
                    }
                }
                ExposedUsersRepo(database)
                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT COUNT(*) FROM users_write_lock WHERE id = 1").use { result ->
                            assertTrue(result.next())
                            assertEquals(1, result.getInt(1))
                        }
                    }
                }
            } finally {
                TransactionManager.closeAndUnregister(database)
            }
        } finally {
            Files.deleteIfExists(databaseFile)
        }
    }

    /** Adding requested-at preserves populated legacy lifecycle slots and leaves historical time unknown. */
    @Test
    fun populatedLegacyLifecycleSchemaSurvivesRepeatedReopenAndSameAddressSaves() = runTest {
        val databaseFile = Files.createTempFile("wishlist-users-legacy-lifecycle", ".sqlite")
        val url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        val approvedId = UserId(41L)
        val firstId = UserId(42L)
        val emptyId = UserId(43L)
        val approvedCurrent = Email("legacy-approved@example.com")
        val approvedPending = Email("legacy-pending@example.com")
        val firstCurrent = Email("legacy-first@example.com")
        val firstReplacement = Email("legacy-first-replacement@example.com")

        fun assertRawLifecycle(firstEmail: Email, firstRequestedAt: Long?) {
            DriverManager.getConnection(url).use { connection ->
                connection.prepareStatement(
                    "SELECT email, email_approved, pending_email, email_change_requested_at, email_change_allowed_at FROM users WHERE id = ?",
                ).use { statement ->
                    fun assertRow(
                        id: UserId,
                        email: Email?,
                        approved: Boolean,
                        pending: Email?,
                        requestedAt: Long?,
                        allowedAt: Long?,
                    ) {
                        statement.setLong(1, id.long)
                        statement.executeQuery().use { result ->
                            assertTrue(result.next())
                            assertEquals(email?.string, result.getString("email"))
                            assertEquals(approved, result.getBoolean("email_approved"))
                            assertEquals(pending?.string, result.getString("pending_email"))
                            val storedRequestedAt = result.getLong("email_change_requested_at").takeUnless { result.wasNull() }
                            val storedAllowedAt = result.getLong("email_change_allowed_at").takeUnless { result.wasNull() }
                            assertEquals(requestedAt, storedRequestedAt)
                            assertEquals(allowedAt, storedAllowedAt)
                        }
                    }

                    assertRow(approvedId, approvedCurrent, true, approvedPending, null, 900L)
                    assertRow(firstId, firstEmail, false, null, firstRequestedAt, null)
                    assertRow(emptyId, null, false, null, null, null)
                }
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM users_write_lock WHERE id = 1 AND marker = 0").use { result ->
                        assertTrue(result.next())
                        assertEquals(1, result.getInt(1))
                    }
                }
            }
        }

        try {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT, email_approved BOOLEAN NOT NULL DEFAULT FALSE, pending_email TEXT, email_change_allowed_at BIGINT)",
                    )
                    statement.execute(
                        "INSERT INTO users (id, username, email, email_approved, pending_email, email_change_allowed_at) VALUES (41, 'legacy-approved', 'legacy-approved@example.com', 1, 'legacy-pending@example.com', 900)",
                    )
                    statement.execute(
                        "INSERT INTO users (id, username, email, email_approved, pending_email, email_change_allowed_at) VALUES (42, 'legacy-first', 'legacy-first@example.com', 0, NULL, NULL)",
                    )
                    statement.execute(
                        "INSERT INTO users (id, username, email, email_approved, pending_email, email_change_allowed_at) VALUES (43, 'legacy-empty', NULL, 0, NULL, NULL)",
                    )
                }
            }

            val firstDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val repo = ExposedUsersRepo(firstDatabase, nowMillis = { 1_000L })
                assertEquals(approvedCurrent, repo.getEmailProfileFresh(approvedId)?.email)
                assertEquals(approvedPending, repo.getEmailProfileFresh(approvedId)?.pendingEmail)
                assertEquals(900L, repo.getEmailProfileFresh(approvedId)?.emailChangeAllowedAt)
                assertNull(repo.getEmailProfileFresh(approvedId)?.emailChangeRequestedAt)
                assertEquals(firstCurrent, repo.getEmailProfileFresh(firstId)?.email)
                assertNull(repo.getEmailProfileFresh(firstId)?.emailChangeRequestedAt)

                assertEquals(
                    listOf(checkNotNull(repo.getById(approvedId))),
                    repo.update(listOf(approvedId to NewUser(Username("legacy-approved"), approvedCurrent))),
                )
                assertEquals(
                    listOf(checkNotNull(repo.getById(approvedId))),
                    repo.update(listOf(approvedId to NewUser(Username("legacy-approved"), approvedPending))),
                )
                assertEquals(
                    listOf(checkNotNull(repo.getById(firstId))),
                    repo.update(listOf(firstId to NewUser(Username("legacy-first"), firstCurrent))),
                )
                assertEquals(
                    listOf(checkNotNull(repo.getById(emptyId))),
                    repo.update(listOf(emptyId to NewUser(Username("legacy-empty"), null))),
                )
                assertRawLifecycle(firstCurrent, null)
            } finally {
                TransactionManager.closeAndUnregister(firstDatabase)
            }

            val reopenedDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val reopened = ExposedUsersRepo(reopenedDatabase, nowMillis = { 1_000L })
                assertEquals(approvedPending, reopened.getEmailProfileFresh(approvedId)?.pendingEmail)
                assertNull(reopened.getEmailProfileFresh(approvedId)?.emailChangeRequestedAt)
                assertEquals(firstCurrent, reopened.getEmailProfileFresh(firstId)?.email)
                assertNull(reopened.getEmailProfileFresh(firstId)?.emailChangeRequestedAt)
                assertRawLifecycle(firstCurrent, null)
            } finally {
                TransactionManager.closeAndUnregister(reopenedDatabase)
            }

            val reopenedTwiceDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val reopenedTwice = ExposedUsersRepo(reopenedTwiceDatabase, nowMillis = { 1_000L })
                val replaced = checkNotNull(reopenedTwice.setEmail(firstId, firstReplacement))
                assertEquals(firstReplacement, replaced.email)
                assertEquals(1_000L, reopenedTwice.getEmailProfileFresh(firstId)?.emailChangeRequestedAt)
                assertRawLifecycle(firstReplacement, 1_000L)
            } finally {
                TransactionManager.closeAndUnregister(reopenedTwiceDatabase)
            }
        } finally {
            Files.deleteIfExists(databaseFile)
        }
    }

    /** Same-address and same-name requests return the retained record without publishing a false update event. */
    @Test
    fun noOpLifecycleWritersDoNotPublishUpdateEvents() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val email = Email("no-op@example.com")
            val created = repo.create(NewUser(Username("no-op"), email)).single()
            val empty = repo.create(NewUser(Username("empty"))).single()
            val approved = checkNotNull(repo.approveEmail(created.id, email))
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertEquals(approved, repo.setEmail(created.id, email))
                assertEquals(approved, repo.updateUsername(created.id, approved.username))
                assertEquals(listOf(approved), repo.update(listOf(created.id to NewUser(approved.username, email))))
                assertEquals(empty, repo.setEmail(empty.id, null))
                advanceUntilIdle()
                assertEquals(emptyList(), events)
            } finally {
                collector.cancel()
            }
        }
    }

    /** Replacements and explicit clears release every previously occupied current or pending slot. */
    @Test
    fun replacementAndClearReleaseCurrentAndPendingAddressClaims() = runTest {
        withInMemorySqliteUsersRepo { repo ->
            val current = Email("release-current@example.com")
            val pending = Email("release-pending@example.com")
            val owner = repo.create(NewUser(Username("owner"), current)).single()
            checkNotNull(repo.approveEmail(owner.id, current))
            checkNotNull(repo.setEmail(owner.id, pending))

            assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("pending-claim"), pending))
            }
            val replacement = checkNotNull(repo.setEmail(owner.id, Email("release-replacement@example.com")))
            assertEquals(Email("release-replacement@example.com"), repo.getEmailProfileFresh(replacement.id)?.pendingEmail)
            assertEquals(pending, repo.create(NewUser(Username("released-pending"), pending)).single().email)

            checkNotNull(repo.setEmail(owner.id, null))
            assertEquals(current, repo.create(NewUser(Username("released-current"), current)).single().email)
            assertEquals(Email("release-replacement@example.com"), repo.create(NewUser(Username("released-replacement"), Email("release-replacement@example.com"))).single().email)

            val deletedCurrent = Email("deleted-current@example.com")
            val deletedPending = Email("deleted-pending@example.com")
            val deletedOwner = repo.create(NewUser(Username("deleted-owner"), deletedCurrent)).single()
            checkNotNull(repo.approveEmail(deletedOwner.id, deletedCurrent))
            checkNotNull(repo.setEmail(deletedOwner.id, deletedPending))
            repo.deleteById(listOf(deletedOwner.id))
            assertEquals(deletedCurrent, repo.create(NewUser(Username("deleted-current-claim"), deletedCurrent)).single().email)
            assertEquals(deletedPending, repo.create(NewUser(Username("deleted-pending-claim"), deletedPending)).single().email)
        }
    }

    /** Independent repositories serialize duplicate current-address creation before either lifecycle read can run. */
    @Test
    fun fileBackedRepositoriesSerializeCurrentAddressClaimsAtTheLock() = runBlocking {
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondLockAcquired = CountDownLatch(1)
        val busyObservation = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstAfterWriteLock = {
                firstLockAcquired.countDown()
                check(releaseFirst.await(15, TimeUnit.SECONDS)) { "first current-address worker was not released" }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
            secondBusyObservation = busyObservation,
        ) { _, first, second ->
            val shared = Email("concurrent-current@example.com")
            val firstWorker = BoundedTestWorker("SQLite first current-address worker") {
                first.create(NewUser(Username("first-current"), shared)).single()
            }
            val secondWorker = BoundedTestWorker("SQLite second current-address worker") {
                second.create(NewUser(Username("second-current"), shared)).single()
            }
            runCleanupProtectedContention(
                workers = listOf(firstWorker, secondWorker),
                cleanupActions = listOf(releaseFirst::countDown, busyObservation::releaseRetry),
            ) {
                firstWorker.start()
                assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
                secondWorker.start()
                assertTrue(busyObservation.awaitBusyEntry())
                busyObservation.assertHealthy()
                assertEquals(1, secondLockAcquired.count)
            }
            busyObservation.assertHealthy()
            assertEquals(shared, firstWorker.result().getOrThrow().email)
            assertIs<DuplicateUserFieldException>(secondWorker.result().exceptionOrNull())
        }
    }

    /** Independent current and pending writers cannot commit the same address, while unrelated queued claims complete. */
    @Test
    fun fileBackedRepositoriesSerializePendingClaimsAndReleaseUnrelatedWriters() = runBlocking {
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondLockAcquired = CountDownLatch(1)
        val busyObservation = SqliteBusyObservation()
        var blockFirstWriter = false
        withFileBackedSqliteUsersRepos(
            firstAfterWriteLock = {
                if (blockFirstWriter) {
                    firstLockAcquired.countDown()
                    check(releaseFirst.await(15, TimeUnit.SECONDS)) { "SQLite pending worker was not released" }
                }
            },
            secondAfterWriteLock = { secondLockAcquired.countDown() },
            secondBusyObservation = busyObservation,
        ) { _, first, second ->
            val current = Email("concurrent-owner@example.com")
            val pending = Email("concurrent-pending@example.com")
            val owner = first.create(NewUser(Username("pending-owner"), current)).single()
            checkNotNull(first.approveEmail(owner.id, current))
            blockFirstWriter = true
            val pendingWorker = BoundedTestWorker("SQLite pending-address worker") {
                first.setEmail(owner.id, pending)
            }
            val currentWorker = BoundedTestWorker("SQLite current-address contender") {
                second.create(NewUser(Username("pending-contender"), pending)).single()
            }
            runCleanupProtectedContention(
                workers = listOf(pendingWorker, currentWorker),
                cleanupActions = listOf(releaseFirst::countDown, busyObservation::releaseRetry),
            ) {
                pendingWorker.start()
                assertTrue(firstLockAcquired.await(10, TimeUnit.SECONDS))
                currentWorker.start()
                assertTrue(busyObservation.awaitBusyEntry())
                busyObservation.assertHealthy()
                assertEquals(1, secondLockAcquired.count)
            }
            busyObservation.assertHealthy()
            checkNotNull(pendingWorker.result().getOrThrow())
            assertEquals(pending, second.getEmailProfileFresh(owner.id)?.pendingEmail)
            assertIs<DuplicateUserFieldException>(currentWorker.result().exceptionOrNull())
            assertEquals(
                Email("unrelated@example.com"),
                second.create(NewUser(Username("unrelated-contender"), Email("unrelated@example.com"))).single().email,
            )
        }
    }

    /** A candidate created after a native lock wait receives the post-acquisition clock value. */
    @Test
    fun blockedCandidateCreateUsesClockAfterObservedLockWait() = runBlocking {
        var now = 1_000L
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstNowMillis = { now },
            firstAfterWriteLock = {
                holderLocked.countDown()
                check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite candidate holder was not released" }
            },
            secondNowMillis = { now },
            secondBusyObservation = busy,
        ) { _, first, second ->
            val holderWorker = BoundedTestWorker("SQLite candidate holder") {
                first.create(NewUser(Username("candidate-holder"), Email("candidate-holder@example.com"))).single()
            }
            val contenderWorker = BoundedTestWorker("SQLite candidate contender") {
                second.create(NewUser(Username("candidate-contender"), Email("candidate-contender@example.com"))).single()
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
                busy.assertHealthy()
                now = 2_000L
            }
            busy.assertHealthy()
            val contender = contenderWorker.result().getOrThrow()
            assertEquals(2_000L, first.getEmailProfileFresh(contender.id)?.emailChangeRequestedAt)
        }
    }

    /** A mutation begun before expiry samples the injected clock only after the observed lock wait ends. */
    @Test
    fun blockedMutationUsesClockAfterLockAtExactExpiry() = runBlocking {
        var now = 500L
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val blockHolder = AtomicBoolean(false)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstNowMillis = { now },
            firstAfterWriteLock = {
                if (blockHolder.get()) {
                    holderLocked.countDown()
                    check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite expiry holder was not released" }
                }
            },
            secondNowMillis = { now },
            secondBusyObservation = busy,
        ) { url, first, second ->
            val address = Email("blocked-expiry@example.com")
            val owner = first.create(NewUser(Username("blocked-expiry"), address)).single()
            checkNotNull(first.approveEmail(owner.id, address, cooldownMillis = 500L))
            now = 999L
            blockHolder.set(true)
            val holderWorker = BoundedTestWorker("SQLite expiry holder") {
                first.create(NewUser(Username("blocked-expiry-holder"), Email("blocked-expiry-holder@example.com")))
            }
            val contenderWorker = BoundedTestWorker("SQLite expiry contender") {
                second.setEmail(owner.id, null)
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
                busy.assertHealthy()
                now = 1_000L
            }
            busy.assertHealthy()
            holderWorker.result().getOrThrow()
            assertNull(contenderWorker.result().getOrThrow()?.email)
            assertRawSqliteAddressHasNoClaim(url, address)
        }
    }

    /** Approval calculates its persisted deadline from the post-lock clock, not the call-start clock. */
    @Test
    fun blockedApprovalIssuesDeadlineFromPostLockClock() = runBlocking {
        var now = 1_000L
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val blockHolder = AtomicBoolean(false)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstNowMillis = { now },
            firstAfterWriteLock = {
                if (blockHolder.get()) {
                    holderLocked.countDown()
                    check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite approval holder was not released" }
                }
            },
            secondNowMillis = { now },
            secondBusyObservation = busy,
        ) { _, first, second ->
            val address = Email("blocked-approval@example.com")
            val owner = first.create(NewUser(Username("blocked-approval"), address)).single()
            blockHolder.set(true)
            val holderWorker = BoundedTestWorker("SQLite approval holder") {
                first.create(NewUser(Username("blocked-approval-holder"), Email("blocked-approval-holder@example.com")))
            }
            val contenderWorker = BoundedTestWorker("SQLite approval contender") {
                second.approveEmail(owner.id, address, cooldownMillis = 500L)
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
                now = 2_000L
            }
            busy.assertHealthy()
            holderWorker.result().getOrThrow()
            checkNotNull(contenderWorker.result().getOrThrow())
            assertEquals(2_500L, second.getEmailProfileFresh(owner.id)?.emailChangeAllowedAt)
        }
    }

    /** A replacement which commits first makes the blocked approval of its former candidate stale. */
    @Test
    fun replacementFirstMakesConcurrentApprovalStale() = runBlocking {
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val blockHolder = AtomicBoolean(false)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstAfterWriteLock = {
                if (blockHolder.get()) {
                    holderLocked.countDown()
                    check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite replacement holder was not released" }
                }
            },
            secondBusyObservation = busy,
        ) { url, first, second ->
            val current = Email("replacement-first-current@example.com")
            val oldPending = Email("replacement-first-old@example.com")
            val replacement = Email("replacement-first-new@example.com")
            val owner = first.create(NewUser(Username("replacement-first"), current)).single()
            checkNotNull(first.approveEmail(owner.id, current))
            checkNotNull(first.setEmail(owner.id, oldPending))
            blockHolder.set(true)
            val holderWorker = BoundedTestWorker("SQLite replacement holder") {
                first.setEmail(owner.id, replacement)
            }
            val contenderWorker = BoundedTestWorker("SQLite stale approval contender") {
                second.approveEmail(owner.id, oldPending)
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
            }
            busy.assertHealthy()
            holderWorker.result().getOrThrow()
            assertNull(contenderWorker.result().getOrThrow())
            val stored = checkNotNull(second.getById(owner.id))
            assertEquals(current, stored.email)
            assertEquals(replacement, second.getEmailProfileFresh(owner.id)?.pendingEmail)
            assertRawSqliteAddressHasOneClaim(url, replacement)
            assertRawSqliteAddressHasNoClaim(url, oldPending)
        }
    }

    /** A positive-cooldown approval which commits first rejects the blocked replacement without partial state. */
    @Test
    fun approvalFirstRejectsConcurrentReplacementDuringPositiveCooldown() = runBlocking {
        var now = 1_000L
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val blockHolder = AtomicBoolean(false)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstNowMillis = { now },
            firstAfterWriteLock = {
                if (blockHolder.get()) {
                    holderLocked.countDown()
                    check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite cooldown approval holder was not released" }
                }
            },
            secondNowMillis = { now },
            secondBusyObservation = busy,
        ) { url, first, second ->
            val initial = Email("approval-first-initial@example.com")
            val candidate = Email("approval-first-candidate@example.com")
            val replacement = Email("approval-first-replacement@example.com")
            val owner = first.create(NewUser(Username("approval-first"), initial)).single()
            checkNotNull(first.approveEmail(owner.id, initial))
            checkNotNull(first.setEmail(owner.id, candidate))
            blockHolder.set(true)
            val holderWorker = BoundedTestWorker("SQLite cooldown approval holder") {
                first.approveEmail(owner.id, candidate, cooldownMillis = 500L)
            }
            val contenderWorker = BoundedTestWorker("SQLite cooldown replacement contender") {
                second.setEmail(owner.id, replacement)
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
            }
            busy.assertHealthy()
            holderWorker.result().getOrThrow()
            assertIs<EmailChangeCooldownException>(contenderWorker.result().exceptionOrNull())
            val stored = checkNotNull(second.getById(owner.id))
            assertEquals(candidate, stored.email)
            assertNull(second.getEmailProfileFresh(owner.id)?.pendingEmail)
            assertEquals(1_500L, second.getEmailProfileFresh(owner.id)?.emailChangeAllowedAt)
            assertRawSqliteAddressHasOneClaim(url, candidate)
            assertRawSqliteAddressHasNoClaim(url, replacement)
        }
    }

    /** A zero-policy approval which commits first still permits the blocked replacement to become pending. */
    @Test
    fun zeroPolicyApprovalFirstAllowsConcurrentReplacement() = runBlocking {
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val blockHolder = AtomicBoolean(false)
        val busy = SqliteBusyObservation()
        withFileBackedSqliteUsersRepos(
            firstAfterWriteLock = {
                if (blockHolder.get()) {
                    holderLocked.countDown()
                    check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite zero-policy holder was not released" }
                }
            },
            secondBusyObservation = busy,
        ) { url, first, second ->
            val initial = Email("zero-first-initial@example.com")
            val candidate = Email("zero-first-candidate@example.com")
            val replacement = Email("zero-first-replacement@example.com")
            val owner = first.create(NewUser(Username("zero-first"), initial)).single()
            checkNotNull(first.approveEmail(owner.id, initial))
            checkNotNull(first.setEmail(owner.id, candidate))
            blockHolder.set(true)
            val holderWorker = BoundedTestWorker("SQLite zero-policy approval holder") {
                first.approveEmail(owner.id, candidate, cooldownMillis = 0L)
            }
            val contenderWorker = BoundedTestWorker("SQLite zero-policy replacement contender") {
                second.setEmail(owner.id, replacement)
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
            }
            busy.assertHealthy()
            holderWorker.result().getOrThrow()
            val stored = contenderWorker.result().getOrThrow()
            assertEquals(candidate, stored?.email)
            assertEquals(replacement, second.getEmailProfileFresh(owner.id)?.pendingEmail)
            assertNull(second.getEmailProfileFresh(owner.id)?.emailChangeAllowedAt)
            assertRawSqliteAddressHasOneClaim(url, replacement)
        }
    }

    /** Xerial retries stop at the callback-owned deadline when a holder never releases before that deadline. */
    @Test
    fun sqliteBusyHandlerStopsRetryingAtAbsoluteDeadline() = runBlocking {
        val holderLocked = CountDownLatch(1)
        val releaseHolder = CountDownLatch(1)
        val busy = SqliteBusyObservation(retryTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(100))
        withFileBackedSqliteUsersRepos(
            firstAfterWriteLock = {
                holderLocked.countDown()
                check(releaseHolder.await(15, TimeUnit.SECONDS)) { "SQLite bounded-retry holder was not released" }
            },
            secondBusyObservation = busy,
        ) { _, first, second ->
            val holderWorker = BoundedTestWorker("SQLite bounded-retry holder") {
                first.create(NewUser(Username("bounded-retry-holder"), Email("bounded-retry-holder@example.com"))).single()
            }
            val contenderWorker = BoundedTestWorker("SQLite bounded-retry contender") {
                second.create(NewUser(Username("bounded-retry-contender"), Email("bounded-retry-contender@example.com"))).single()
            }
            runCleanupProtectedContention(
                workers = listOf(holderWorker, contenderWorker),
                cleanupActions = listOf(releaseHolder::countDown, busy::releaseRetry),
            ) {
                holderWorker.start()
                assertTrue(holderLocked.await(10, TimeUnit.SECONDS))
                contenderWorker.start()
                assertTrue(busy.awaitBusyEntry())
                assertTrue(busy.awaitRetryDeadlineExhaustion())
            }
            busy.assertRetryDeadlineExhausted()
            holderWorker.result().getOrThrow()
        }
    }

    /** A real SQLite writer lock failure stays infrastructure-visible instead of becoming duplicate or cooldown feedback. */
    @Test
    fun sqliteWriterContentionIsNotClassifiedAsLifecycleFeedback() = runTest {
        withFileBackedSqliteUsersRepos { url, first, _ ->
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA busy_timeout = 0")
                    statement.execute("BEGIN IMMEDIATE")
                    try {
                        val failure = assertFailsWith<ExposedSQLException> {
                            first.create(NewUser(Username("contention"), Email("contention@example.com")))
                        }
                        val sqlite = assertIs<SQLiteException>(failure.cause)
                        assertEquals(SQLiteErrorCode.SQLITE_BUSY, sqlite.resultCode)
                    } finally {
                        statement.execute("ROLLBACK")
                    }
                }
            }
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

    /** Explicit clears reject before mutation during cooldown and reset every lifecycle field at expiry. */
    @Test
    fun clearLifecycleHonorsDeadlineAcrossMutationEntryPoints() = runTest {
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val address = Email("clear-policy@example.com")
            val user = repo.create(NewUser(Username("clear-policy"), address)).single()
            val approved = checkNotNull(repo.approveEmail(user.id, address, cooldownMillis = 500L))

            assertFailsWith<EmailChangeCooldownException> { repo.setEmail(user.id, null) }
            assertFailsWith<EmailChangeCooldownException> {
                repo.update(user.id, NewUser(Username("must-not-rename"), null))
            }
            assertEquals(approved, repo.getById(user.id))

            now = 1_500L
            val cleared = checkNotNull(repo.update(user.id, NewUser(Username("cleared"), null)))
            assertEquals(Username("cleared"), cleared.username)
            assertNull(cleared.email)
            assertFalse(cleared.emailApproved)
            assertEquals(EmailProfile(userId = user.id.long), repo.getEmailProfileFresh(user.id))

            val unapproved = repo.create(NewUser(Username("unapproved"), Email("unapproved-clear@example.com"))).single()
            assertNull(checkNotNull(repo.setEmail(unapproved.id, null)).email)
        }
    }

    /** A restricted later bulk clear rolls back an earlier rename and publishes no update events. */
    @Test
    fun batchClearRejectionRollsBackEarlierMutationAndEvents() = runTest {
        var now = 1_000L
        withInMemorySqliteUsersRepo(nowMillis = { now }) { repo ->
            val first = repo.create(NewUser(Username("clear-first"))).single()
            val address = Email("clear-batch@example.com")
            val restricted = repo.create(NewUser(Username("clear-restricted"), address)).single()
            checkNotNull(repo.approveEmail(restricted.id, address, cooldownMillis = 500L))
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertFailsWith<EmailChangeCooldownException> {
                    repo.update(
                        listOf(
                            first.id to NewUser(Username("must-roll-back"), null),
                            restricted.id to NewUser(restricted.username, null),
                        ),
                    )
                }
                advanceUntilIdle()
                assertEquals(first, repo.getById(first.id))
                assertEquals(address, repo.getById(restricted.id)?.email)
                assertTrue(events.isEmpty())
            } finally {
                collector.cancel()
            }
        }
    }

    /** A non-representable approval deadline rolls back the row and emits no speculative event. */
    @Test
    fun approvalDeadlineOverflowRollsBackWithoutPublishingAnEvent() = runTest {
        withInMemorySqliteUsersRepo(nowMillis = { Long.MAX_VALUE - 5L }) { repo ->
            val email = Email("approval-overflow@example.com")
            val user = repo.create(NewUser(Username("approval-overflow"), email)).single()
            val before = checkNotNull(repo.getById(user.id))
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                repo.updatedObjectsFlow.collect(events::add)
            }
            try {
                assertFailsWith<ArithmeticException> {
                    repo.approveEmail(user.id, email, cooldownMillis = 10L)
                }

                advanceUntilIdle()
                assertEquals(before, repo.getById(user.id))
                assertTrue(events.isEmpty())
            } finally {
                collector.cancel()
            }
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

    /** Reads the durable cross-slot ownership count through an independent JDBC connection. */
    private fun assertRawSqliteAddressHasOneClaim(url: String, address: Email) {
        assertRawSqliteAddressClaimCount(url, address, expected = 1)
    }

    /** Reads the durable cross-slot ownership absence through an independent JDBC connection. */
    private fun assertRawSqliteAddressHasNoClaim(url: String, address: Email) {
        assertRawSqliteAddressClaimCount(url, address, expected = 0)
    }

    private fun assertRawSqliteAddressClaimCount(url: String, address: Email, expected: Int) {
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ? OR pending_email = ?").use { statement ->
                statement.setString(1, address.string)
                statement.setString(2, address.string)
                statement.executeQuery().use { row ->
                    assertTrue(row.next())
                    assertEquals(expected, row.getInt(1))
                }
            }
        }
    }

    /** Runs one lifecycle worker on a daemon thread and retains its terminal result for assertions. */
    private inner class BoundedTestWorker<T>(
        /** Human-readable identity included in timeout failures. */
        private val description: String,
        /** Suspending lifecycle operation whose terminal value or failure must be asserted by the test. */
        private val action: suspend () -> T,
    ) {
        /** Records whether [start] successfully handed the worker to the JVM. */
        private val started = AtomicBoolean(false)

        /** Signals completion independently from a finite join operation. */
        private val completed = CountDownLatch(1)

        /** Retains the worker value or failure without rethrowing on the worker thread. */
        private val outcome = AtomicReference<Result<T>?>(null)

        /** Daemon containment prevents a failed test from leaving a non-daemon JVM worker alive. */
        private val thread = Thread(
            {
                try {
                    outcome.set(runCatching { runBlocking { action() } })
                } finally {
                    completed.countDown()
                }
            },
            description,
        ).apply { isDaemon = true }

        /** Starts the worker exactly once while the caller's cleanup guard is already active. */
        fun start() {
            check(started.compareAndSet(false, true)) { "$description was started more than once" }
            thread.start()
        }

        /** Returns the recorded terminal outcome after cleanup has proven worker completion. */
        fun result(): Result<T> = checkNotNull(outcome.get()) { "$description produced no terminal outcome" }

        /** Finite cleanup that keeps later releases and joins running after an earlier failure. */
        fun awaitAndJoin(cleanupFailures: MutableList<Throwable>) {
            if (!started.get()) return
            collectCleanupFailure(cleanupFailures) {
                assertTrue(completed.await(15, TimeUnit.SECONDS), "$description did not signal completion")
            }
            collectCleanupFailure(cleanupFailures) {
                thread.join(TimeUnit.SECONDS.toMillis(15))
            }
            if (thread.isAlive) {
                collectCleanupFailure(cleanupFailures) {
                    thread.interrupt()
                    thread.join(TimeUnit.SECONDS.toMillis(15))
                }
            }
            collectCleanupFailure(cleanupFailures) {
                assertFalse(thread.isAlive, "$description outlived bounded cleanup")
                checkNotNull(outcome.get()) { "$description completed without a recorded outcome" }
            }
        }
    }

    /** Runs a contention body with unconditional gate release and aggregate worker cleanup. */
    private inline fun <T> runCleanupProtectedContention(
        workers: List<BoundedTestWorker<*>>,
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
            cleanupActions.forEach { action -> collectCleanupFailure(cleanupFailures, action) }
            workers.forEach { worker -> worker.awaitAndJoin(cleanupFailures) }
            if (cleanupFailures.isNotEmpty()) {
                val cleanupFailure = AssertionError("contention worker cleanup failed")
                cleanupFailures.forEach(cleanupFailure::addSuppressed)
                primaryFailure?.addSuppressed(cleanupFailure)
                if (primaryFailure == null) throw cleanupFailure
            }
        }
    }

    /** Adds one cleanup failure without preventing subsequent releases, waits, or joins. */
    private fun collectCleanupFailure(cleanupFailures: MutableList<Throwable>, action: () -> Unit) {
        runCatching(action).exceptionOrNull()?.let(cleanupFailures::add)
    }

}
