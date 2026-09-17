package dev.inmo.wishlist.features.users.common.repo

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.DriverManager
import java.nio.file.Files
import java.util.UUID

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
    block: suspend (String, ExposedUsersRepo, ExposedUsersRepo) -> Unit,
) {
    val databaseFile = Files.createTempFile("wishlist-users-concurrent", ".sqlite")
    val url = "jdbc:sqlite:file:${databaseFile.toAbsolutePath()}?busy_timeout=5000"
    val firstDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
    val secondDatabase = Database.connect(url = url, driver = "org.sqlite.JDBC")
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
