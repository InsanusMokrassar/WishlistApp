package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies [DisabledEmailFeature]: [DisabledEmailFeature.isFeatureEnabled]/
 * [DisabledEmailFeature.sendTestEmail] are hard no-ops; [DisabledEmailFeature.setMyEmail] still
 * persists via `UsersRepo` — storage stays independent of SMTP configuration.
 */
class DisabledEmailFeatureTest {

    /** Fixture user whose username is the privileged "root" account. */
    private val rootUser = RegisteredUser(UserId(1L), Username("root"))

    /** Fixture user whose username is an ordinary, non-root account. */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Always `false` — trivial no-op. */
    @Test
    fun isFeatureEnabledReturnsFalse() = runTest {
        val feature = DisabledEmailFeature(FakeUsersRepo())
        assertFalse(feature.isFeatureEnabled())
    }

    /** `false` even for a caller who genuinely is root — there is no SMTP transport to use. */
    @Test
    fun sendTestEmailReturnsFalseForRootCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val feature = DisabledEmailFeature(repo)

        assertFalse(feature.sendTestEmail(rootUser.id, Email("recipient@example.com")))
    }

    /** `false` for a non-root caller and for a caller id that doesn't resolve to any user. */
    @Test
    fun sendTestEmailReturnsFalseForNonRootOrMissingCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val feature = DisabledEmailFeature(repo)

        assertFalse(feature.sendTestEmail(plainUser.id, Email("recipient@example.com")))
        assertFalse(feature.sendTestEmail(UserId(999L), Email("recipient@example.com")))
    }

    /** A found user's stored email is updated and the call reports success. */
    @Test
    fun setMyEmailPersistsViaUsersRepoWhenUserFound() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val feature = DisabledEmailFeature(repo)
        val newEmail = Email("alice@example.com")

        val result = feature.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Passing `null` clears a previously-stored email address. */
    @Test
    fun setMyEmailClearsStoredEmailWhenPassedNull() = runTest {
        val userWithEmail = plainUser.copy(email = Email("alice@example.com"))
        val repo = FakeUsersRepo(mapOf(userWithEmail.id to userWithEmail))
        val feature = DisabledEmailFeature(repo)

        val result = feature.setMyEmail(userWithEmail.id, null)

        assertTrue(result)
        assertNull(repo.getById(userWithEmail.id)?.email)
    }

    /** A caller id that doesn't resolve to any user reports failure and updates nothing. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val feature = DisabledEmailFeature(FakeUsersRepo())

        assertFalse(feature.setMyEmail(UserId(999L), Email("alice@example.com")))
    }
}
