package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] always returns `true`
 * (this class is only ever constructed with a real, non-null `EmailsService` — see
 * `DisabledEmailFeatureTest` for the SMTP-disabled no-op path); [EmailFeatureService.sendTestEmail]
 * enforces the root-only guard before delegating exactly one `EmailsService.sendText` call;
 * [EmailFeatureService.setMyEmail] persists via `UsersRepo` for a found user.
 */
class EmailFeatureServiceTest {

    /** Fixture user whose username is the privileged "root" account. */
    private val rootUser = RegisteredUser(UserId(1L), Username("root"))

    /** Fixture user whose username is an ordinary, non-root account. */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    /** `isFeatureEnabled` unconditionally returns `true` — `emailsService` is now a non-nullable constructor parameter, so this class is only ever constructed with a real transport. */
    @Test
    fun isFeatureEnabledAlwaysReturnsTrue() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())
        assertTrue(service.isFeatureEnabled())
    }

    /** Root caller + present `emailsService` → exactly one `sendText` call with the fixed subject/text; result is `sendText`'s own `true`. */
    @Test
    fun sendTestEmailDelegatesToSendTextForRootCallerAndReturnsTrueResult() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val emailsService = FakeEmailsService(result = true)
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(rootUser.id, recipient)

        assertTrue(result)
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
    fun sendTestEmailDelegatesToSendTextForRootCallerAndReturnsFalseResult() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val emailsService = FakeEmailsService(result = false)
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(rootUser.id, recipient)

        assertFalse(result)
        assertEquals(1, emailsService.sendTextCalls.size)
    }

    /** Non-root caller → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseForNonRootCallerAndDoesNotCallSendText() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val emailsService = FakeEmailsService()
        val service = EmailFeatureService(emailsService, repo)

        val result = service.sendTestEmail(plainUser.id, recipient)

        assertFalse(result)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** Caller id resolves to no user → `false`, and `sendText` must never be invoked. */
    @Test
    fun sendTestEmailReturnsFalseWhenCallerNotFound() = runTest {
        val emailsService = FakeEmailsService()
        val service = EmailFeatureService(emailsService, FakeUsersRepo())

        val result = service.sendTestEmail(UserId(999L), recipient)

        assertFalse(result)
        assertEquals(0, emailsService.sendTextCalls.size)
    }

    /** A found user's stored email is updated and persisted via `UsersRepo`, exercising `EmailFeatureService.setMyEmail` directly (the SMTP-disabled path is covered separately by `DisabledEmailFeatureTest`). */
    @Test
    fun setMyEmailPersistsViaUsersRepoForFoundUser() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(FakeEmailsService(), repo)
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    /** Caller id resolves to no user → `setMyEmail` returns `false`. */
    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }
}
