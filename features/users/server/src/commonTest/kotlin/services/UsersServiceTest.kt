package dev.inmo.wishlist.features.users.server.services

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.micro_utils.repos.ReadMapCRUDRepo
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
    initialUsers: Map<UserId, RegisteredUser> = emptyMap(),
    private val emailProfiles: Map<UserId, EmailProfile> = emptyMap(),
) : ReadUsersRepo, ReadCRUDRepo<RegisteredUser, UserId> by ReadMapCRUDRepo(initialUsers) {

    /**
     * Linear scan over all stored users for one matching [username].
     *
     * @param username Username to look up.
     * @return Matching user, or `null` when none is stored.
     */
    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        getAll().values.firstOrNull { it.username == username }

    /** Returns independently seeded email-owned lifecycle state. */
    override suspend fun getEmailProfileFresh(id: UserId): EmailProfile? =
        emailProfiles[id]
}

/**
 * Verifies [UsersService.getAll] projects every stored [RegisteredUser] onto [UsersFeatureUser],
 * dropping private email data — the regression test for the issue #67 public-listing leak.
 */
class UsersServiceTest {

    /** Fixture user carrying current private identity state. */
    private val userWithEmail = RegisteredUser(
        UserId(7L),
        Username("bob"),
        Email("approved@example.com"),
        emailApproved = true,
    )

    /** Independently persisted lifecycle state that must never affect a public response. */
    private val pendingEmailProfile = EmailProfile(
        userId = userWithEmail.id.long,
        email = userWithEmail.email,
        emailApproved = true,
        pendingEmail = Email("pending@example.com"),
        emailChangeRequestedAt = 1_798_761_500_000L,
        emailChangeAllowedAt = 1_798_761_600_000L,
    )

    /** A seeded user with a non-null email is returned as a [UsersFeatureUser] with only id/username. */
    @Test
    fun getAllReturnsFeatureModelDroppingEmail() = runTest {
        val service = UsersService(FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail)))

        val result = service.getAll()

        // UsersFeatureUser has no `email` property at all, so this equality check is also a
        // compile-time proof that the returned type cannot carry the email through.
        assertEquals(listOf(UsersFeatureUser(userWithEmail.id, userWithEmail.username)), result)
    }

    /** Public JSON omits populated lifecycle state persisted separately in [EmailProfile]. */
    @Test
    fun listAndSinglePublicResponsesOmitPopulatedPrivateMetadata() = runTest {
        val repo = FakeUsersRepo(
            initialUsers = mapOf(userWithEmail.id to userWithEmail),
            emailProfiles = mapOf(userWithEmail.id to pendingEmailProfile),
        )
        val result = UsersService(repo).getAll()

        val listElement = Json.encodeToJsonElement(ListSerializer(UsersFeatureUser.serializer()), result)
            .jsonArray
            .single()
            .jsonObject
        val single = Json.encodeToJsonElement(UsersFeatureUser.serializer(), result.single()).jsonObject

        assertEquals(setOf("id", "username"), listElement.keys)
        assertEquals(setOf("id", "username"), single.keys)
        assertEquals(pendingEmailProfile, repo.getEmailProfileFresh(userWithEmail.id))
    }

    /** An empty repo maps to an empty list. */
    @Test
    fun getAllReturnsEmptyListWhenRepoEmpty() = runTest {
        val service = UsersService(FakeUsersRepo())

        assertTrue(service.getAll().isEmpty())
    }
}
