package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies duplicate-field translation through the real Exposed and Xerial SQLite stack. */
class ExposedUsersRepoSqliteTest {
    /** A duplicate username maps to the repository's existing duplicate-field contract. */
    @Test
    fun createDuplicateUsernameMapsToDuplicateUserFieldException() = runTest {
        withSqliteRepo { repo ->
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
        withSqliteRepo { repo ->
            val seeded = repo.create(
                NewUser(Username("alice"), Email("shared@example.com"))
            ).single()
            val before = repo.getAll()

            val failure = assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("bob"), Email("shared@example.com")))
            }

            assertSqliteUniqueCause(failure)
            assertEquals(mapOf(seeded.id to seeded), before)
            assertEquals(before, repo.getAll())
        }
    }

    /** A username collision during update is translated without mutating either stored row. */
    @Test
    fun updateDuplicateUsernameMapsToDuplicateUserFieldException() = runTest {
        withSqliteRepo { repo ->
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
        withSqliteRepo { repo ->
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

            assertSqliteUniqueCause(failure)
            assertEquals(before, repo.getAll())
        }
    }

    /** SQLite permits multiple distinct users to retain null email values. */
    @Test
    fun multipleNullEmailsRemainValid() = runTest {
        withSqliteRepo { repo ->
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

    /** A real non-constraint SQLite failure preserves the original Exposed exception type. */
    @Test
    fun nonUniqueDatabaseFailureRemainsExposedSQLException() = runTest {
        withSqliteRepo { repo ->
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

    /**
     * Provides a uniquely named shared in-memory SQLite database whose keeper connection outlives
     * every Exposed transaction in [block].
     *
     * @param block Repository assertions to run before guaranteed database cleanup.
     */
    private suspend fun withSqliteRepo(block: suspend (ExposedUsersRepo) -> Unit) {
        val url = "jdbc:sqlite:file:users-${UUID.randomUUID()}?mode=memory&cache=shared"
        val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
        val keeper = try {
            DriverManager.getConnection(url)
        } catch (error: Throwable) {
            TransactionManager.closeAndUnregister(database)
            throw error
        }

        try {
            block(ExposedUsersRepo(database))
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                keeper.close()
            }
        }
    }
}
