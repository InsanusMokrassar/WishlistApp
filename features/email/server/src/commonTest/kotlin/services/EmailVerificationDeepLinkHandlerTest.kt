package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Verifies email approval handler type, existence, and idempotent role behavior. */
class EmailVerificationDeepLinkHandlerTest {
    private val user = RegisteredUser(UserId(7L), Username("alice"), Email("alice@example.com"))
    private val deeplinkId = DeepLinkId("verification-7")

    /**
     * Builds a handler using one coordinator for [usersRepo] and [rolesRepo].
     *
     * @param usersRepo User state read by verification.
     * @param rolesRepo Role state updated by verification.
     * @return Handler backed by the shared coordinator.
     */
    private fun createHandler(
        usersRepo: FakeUsersRepo,
        rolesRepo: FakeRolesRepo,
        emailsService: FakeEmailsService? = null,
    ): EmailVerificationDeepLinkHandler = EmailVerificationDeepLinkHandler(
        EmailVerificationAccountCoordinator(usersRepo, rolesRepo),
        emailsService,
    )

    /** Wrong payload type is unhandled. */
    @Test
    fun wrongPayloadTypeIsRejected() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertEquals(null, handler.tryHandle(deeplinkId, "wrong"))
        assertTrue(roles.contains(subject, NewUserRole))
        assertEquals(false, roles.contains(subject, UserRole))
    }

    /** A missing account is unhandled. */
    @Test
    fun missingUserIsRejected() = runTest {
        val handler = createHandler(FakeUsersRepo(), FakeRolesRepo())

        assertEquals(null, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
    }

    /** Existing pending accounts become approved and remain approved on repeated opens. */
    @Test
    fun existingUserIsPromotedIdempotentlyAndConfirmationIsRetried() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val emails = FakeEmailsService()
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles, emails)

        val expected = HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath)
        assertEquals(expected, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertEquals(expected, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, UserRole))
        assertEquals(false, roles.contains(subject, NewUserRole))
        assertEquals(2, emails.sendTextCalls.size)
        assertEquals(user.email, emails.sendTextCalls.first().recipient)
        assertEquals("Your WishlistApp account is approved", emails.sendTextCalls.first().subject)
    }

    /** A link sent to an earlier address cannot approve an account after the address changes. */
    @Test
    fun changedEmailRejectsStaleVerificationLink() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val changed = user.copy(email = Email("changed@example.com"))
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to changed)), roles)

        assertEquals(null, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertEquals(false, roles.contains(subject, UserRole))
    }

    /** Clearing the stored address leaves the pending account unapproved. */
    @Test
    fun clearedEmailRejectsVerificationLink() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val cleared = user.copy(email = null)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to cleared)), roles)

        assertEquals(null, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertEquals(false, roles.contains(subject, UserRole))
    }

    /** Persisted user-id-only payloads decode compatibly but fail closed at handling time. */
    @Test
    fun legacyPayloadWithoutEmailIsRejected() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertEquals(null, handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertEquals(false, roles.contains(subject, UserRole))
    }

    /** SMTP absence, false results, and ordinary failures preserve the completed redirect. */
    @Test
    fun deliveryFailuresDoNotUndoApprovalRedirect() = runTest {
        listOf(
            null,
            FakeEmailsService(result = false),
            FakeEmailsService(failure = IllegalStateException("SMTP failure")),
        ).forEach { emailsService ->
            val roles = FakeRolesRepo()
            val subject = BaseRoleSubject.Direct(user.id.long.toString())
            roles.includeDirect(subject, NewUserRole)
            val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles, emailsService)

            assertEquals(
                HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath),
                handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)),
            )
            assertTrue(roles.contains(subject, UserRole))
        }
    }

    /** Cancellation continues to the caller after approval has already been committed. */
    @Test
    fun cancellationPropagatesAfterApproval() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(
            FakeUsersRepo(mapOf(user.id to user)),
            roles,
            FakeEmailsService(failure = CancellationException("cancel")),
        )

        assertFailsWith<CancellationException> {
            handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email))
        }
        assertTrue(roles.contains(subject, UserRole))
    }
}
