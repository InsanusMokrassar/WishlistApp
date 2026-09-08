package dev.inmo.wishlist.features.users.common.repo

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.DriverManager
import java.util.UUID

/** Runs [block] against an isolated in-memory SQLite users repository. */
internal suspend fun withInMemorySqliteUsersRepo(block: suspend (ExposedUsersRepo) -> Unit) {
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
