package dev.inmo.wishlist.features.users.server.services

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.micro_utils.repos.ReadMapCRUDRepo
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-memory [ReadUsersRepo] test double. [UsersService] only needs read access, so this delegates
 * pagination/lookup entirely to a [ReadMapCRUDRepo] over a fixed seed map instead of pulling in the
 * full write-capable [dev.inmo.micro_utils.repos.MapCRUDRepo] machinery
 * (`features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt` uses the latter because
 * `EmailFeatureService` also writes through `UsersRepo`; this one does not).
 *
 * @param initialUsers Users the repo is pre-seeded with, keyed by their [UserId].
 */
internal class FakeUsersRepo(
    initialUsers: Map<UserId, RegisteredUser> = emptyMap()
) : ReadUsersRepo, ReadCRUDRepo<RegisteredUser, UserId> by ReadMapCRUDRepo(initialUsers) {

    /**
     * Linear scan over all stored users for one matching [username].
     *
     * @param username Username to look up.
     * @return Matching user, or `null` when none is stored.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }
}

/**
 * Verifies [UsersService.getAll] projects every stored [RegisteredUser] onto [UsersFeatureUser],
 * dropping [RegisteredUser.email] — the regression test for the issue #67 public-listing leak.
 */
class UsersServiceTest {

    /** Fixture user carrying a non-null email, to prove the returned model cannot expose it. */
    private val userWithEmail = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

    /** A seeded user with a non-null email is returned as a [UsersFeatureUser] with only id/username. */
    @Test
    fun getAllReturnsFeatureModelDroppingEmail() = runTest {
        val service = UsersService(FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail)))

        val result = service.getAll()

        // UsersFeatureUser has no `email` property at all, so this equality check is also a
        // compile-time proof that the returned type cannot carry the email through.
        assertEquals(listOf(UsersFeatureUser(userWithEmail.id, userWithEmail.username)), result)
    }

    /** An empty repo maps to an empty list. */
    @Test
    fun getAllReturnsEmptyListWhenRepoEmpty() = runTest {
        val service = UsersService(FakeUsersRepo())

        assertTrue(service.getAll().isEmpty())
    }
}
