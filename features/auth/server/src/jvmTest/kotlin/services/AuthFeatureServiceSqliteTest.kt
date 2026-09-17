package dev.inmo.wishlist.features.auth.server.services

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies authenticated lifecycle reads against independently connected real SQLite repositories. */
class AuthFeatureServiceSqliteTest {
    /** Allows credential issuance and token resolution for the fixture-owned account. */
    private object AllowingUserRoleAuthorization : UserRoleAuthorization {
        /** Accepts direct-role provisioning for fixture accounts. */
        override suspend fun ensureUserRole(userId: UserId): Boolean = true

        /** Reports direct-role membership for fixture accounts. */
        override suspend fun hasUserRole(userId: UserId): Boolean = true
    }

    /**
     * Keeps a live cache stale through an independent writer while the authenticated read bypasses
     * that cache and returns the complete current, approval, pending, and deadline lifecycle.
     */
    @Test
    fun authenticatedPrivateReadBypassesLiveIndependentlyStaleCache() = runTest {
        val current = Email("approved@example.com")
        val pending = Email("pending@example.com")
        var now = 1_000L
        val databaseFile = Files.createTempFile("wishlist-auth-users", ".sqlite")
        val firstDatabase = Database.connect(
            url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}",
            driver = "org.sqlite.JDBC",
        )
        val secondDatabase = Database.connect(
            url = "jdbc:sqlite:${databaseFile.toAbsolutePath()}",
            driver = "org.sqlite.JDBC",
        )
        val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val firstRepo = ExposedUsersRepo(firstDatabase, nowMillis = { now })
            val secondRepo = ExposedUsersRepo(secondDatabase, nowMillis = { now })
            val initial = firstRepo.create(listOf(NewUser(Username("bob"), current))).single()
            val cache = CacheUsersRepo(firstRepo, cacheScope)
            val service = AuthFeatureService(
                usersRepo = cache,
                writeUsersRepo = cache,
                passwordsRepo = FakePasswordsRepo(),
                userRoleAuthorization = AllowingUserRoleAuthorization,
            )
            service.setPassword(initial.id, Password("s3cret-pw"))
            val credentials = checkNotNull(service.login(initial.username, Password("s3cret-pw")))

            assertEquals(initial, cache.getById(initial.id))
            val approved = checkNotNull(secondRepo.approveEmail(initial.id, current, cooldownMillis = 5L))
            assertEquals(1_005L, approved.emailChangeAllowedAt)
            now = checkNotNull(approved.emailChangeAllowedAt)
            val replacement = checkNotNull(secondRepo.setEmail(initial.id, pending))

            assertEquals(initial, cache.getById(initial.id))
            assertEquals(
                AuthFeatureUser(
                    id = initial.id,
                    username = initial.username,
                    email = current,
                    emailApproved = true,
                    pendingEmail = pending,
                    emailChangeAllowedAt = approved.emailChangeAllowedAt,
                ),
                service.getUser(credentials.token),
            )
            assertEquals(replacement, cache.getByIdFresh(initial.id))
        } finally {
            cacheScope.cancel()
            try {
                TransactionManager.closeAndUnregister(secondDatabase)
            } finally {
                try {
                    TransactionManager.closeAndUnregister(firstDatabase)
                } finally {
                    Files.deleteIfExists(databaseFile)
                }
            }
        }
    }
}
