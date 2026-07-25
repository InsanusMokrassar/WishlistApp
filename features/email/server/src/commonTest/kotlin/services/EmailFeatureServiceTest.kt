package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] always returns `true` (this
 * class is only ever constructed with a real, non-null `EmailsService` — see `DisabledEmailFeatureTest`
 * for the SMTP-disabled no-op path); [EmailFeatureService.sendTestEmail] delegates the privilege check
 * to `RolesFeature.isFunctionalityAvailable(callerId, email.sendTest)` before delegating exactly one
 * `EmailsService.sendText` call (an unknown `UserId` takes the same "not available" branch as any other
 * non-privileged caller); [EmailFeatureService.setMyEmail] persists via `UsersRepo` for a found user,
 * unaffected by the role check.
 */
class EmailFeatureServiceTest {

    /** Fixture user used by every `setMyEmail` assertion (superadmin status is irrelevant there). */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    /** `isFeatureEnabled` unconditionally returns `true` — `emailsService` is a non-nullable constructor parameter, so this class is only ever constructed with a real transport. */
    @Test
    fun isFeatureEnabledAlwaysReturnsTrue() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo(), FakeRolesFeature())
        assertTrue(service.isFeatureEnabled())
    }

    /** Superadmin caller + present `emailsService` → exactly one `sendText` call with the fixed subject/text; result is `sendText`'s own `true`. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsTrueResult() = runTest {
        val emailsService = FakeEmailsService(result = true)
        val rolesFeature = FakeRolesFeature(result = true)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), rolesFeature)

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertTrue(result)
        assertEquals(listOf(plainUser.id to EmailConstants.sendTestFunctionalityId), rolesFeature.calls)
        assertEquals(1, emailsService.sendTextCalls.size)
        val call = emailsService.sendTextCalls.single()
        assertEquals(recipient, call.recipient)
        assertEquals("Test email from WishlistApp", call.subject)
        assertEquals(
            "This is a test email sent from WishlistApp to verify SMTP configuration.",
            call.text
        )
    }

    /** Same as above but `sendText` fails — the `false` result must propagate through unchanged. */
    @Test
    fun sendTestEmailDelegatesToSendTextForSuperAdminCallerAndReturnsFalseResult() = runTest {
        val emailsService = FakeEmailsService(result = false)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), FakeRolesFeature(result = true))

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertFalse(result)
        assertEquals(1, emailsService.sendTextCalls.size)
    }

    /** Non-superadmin caller (including an id unknown to `usersRepo`, now indistinguishable from any other non-superadmin caller) → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseWhenCallerIsNotSuperAdminAndDoesNotCallSendText() = runTest {
        val emailsService = FakeEmailsService()
        val rolesFeature = FakeRolesFeature(result = false)
        val service = EmailFeatureService(emailsService, FakeUsersRepo(), rolesFeature)

        val result = service.sendTestEmail(UserId(999L), recipient)

        assertFalse(result)
        assertEquals(listOf(UserId(999L) to EmailConstants.sendTestFunctionalityId), rolesFeature.calls)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** A found user's stored email is updated and persisted via `UsersRepo`, exercising `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by `DisabledEmailFeatureTest`); superadmin status is irrelevant to this method. */
    @Test
    fun setMyEmailPersistsViaUsersRepoForFoundUser() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo, FakeRolesFeature())
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Caller id resolves to no user → `setMyEmail` returns `false`. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo(), FakeRolesFeature())

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }

    /** setMyEmail propagates DuplicateUserFieldException, unmodified, when the target email is already stored for a different user. */
    @Test
    fun setMyEmailPropagatesDuplicateUserFieldExceptionWhenEmailAlreadyTaken() = runTest {
        val takenEmail = Email("taken@example.com")
        val ownerUser = plainUser.copy(id = UserId(1L), email = takenEmail)
        val repo = FakeUsersRepo(mapOf(ownerUser.id to ownerUser, plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo, FakeRolesFeature())

        assertFailsWith<DuplicateUserFieldException> {
            service.setMyEmail(plainUser.id, takenEmail)
        }
    }
}
