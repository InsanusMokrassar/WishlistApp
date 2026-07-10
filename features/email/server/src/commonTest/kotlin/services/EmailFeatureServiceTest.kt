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
 * Verifies [EmailFeatureService]: [EmailFeatureService.isFeatureEnabled] reports
 * `emailsService != null`; [EmailFeatureService.sendTestEmail] enforces the root-only guard before
 * delegating exactly one `EmailsService.sendText` call; [EmailFeatureService.setMyEmail] persists
 * via `UsersRepo` regardless of `EmailsService` availability (storage stays independent of SMTP).
 */
class EmailFeatureServiceTest {

    /** Fixture user whose username is the privileged "root" account. */
    private val rootUser = RegisteredUser(UserId(1L), Username("root"))

    /** Fixture user whose username is an ordinary, non-root account. */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Shared test-email recipient used by every `sendTestEmail` assertion. */
    private val recipient = Email("recipient@example.com")

    @Test
    fun isFeatureEnabledTrueWhenEmailsServiceNonNull() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())
        assertTrue(service.isFeatureEnabled())
    }

    @Test
    fun isFeatureEnabledFalseWhenEmailsServiceNull() = runTest {
        val service = EmailFeatureService(null, FakeUsersRepo())
        assertFalse(service.isFeatureEnabled())
    }

    /** `emailsService == null` short-circuits before the root check even runs its course. */
    @Test
    fun sendTestEmailReturnsFalseWhenEmailsServiceNullEvenForRootCaller() = runTest {
        val repo = FakeUsersRepo(mapOf(rootUser.id to rootUser))
        val service = EmailFeatureService(null, repo)

        assertFalse(service.sendTestEmail(rootUser.id, recipient))
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

    /** Storage keeps working even when `emailsService` is `null` — proves the documented independence. */
    @Test
    fun setMyEmailPersistsViaUsersRepoRegardlessOfEmailsService() = runTest {
        val repo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val service = EmailFeatureService(null, repo)
        val newEmail = Email("alice@example.com")

        val result = service.setMyEmail(plainUser.id, newEmail)

        assertTrue(result)
        assertEquals(newEmail, repo.getById(plainUser.id)?.email)
    }

    @Test
    fun setMyEmailReturnsFalseWhenUserNotFound() = runTest {
        val service = EmailFeatureService(FakeEmailsService(), FakeUsersRepo())

        assertFalse(service.setMyEmail(UserId(999L), Email("alice@example.com")))
    }
}
