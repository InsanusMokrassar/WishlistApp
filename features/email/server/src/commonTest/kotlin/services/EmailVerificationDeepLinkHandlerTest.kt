package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
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
    ): EmailVerificationDeepLinkHandler = EmailVerificationDeepLinkHandler(
        EmailVerificationAccountCoordinator(usersRepo, rolesRepo),
    )

    /** Wrong payload type is unhandled. */
    @Test
    fun wrongPayloadTypeIsRejected() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertFalse(handler.tryHandle(deeplinkId, "wrong"))
        assertTrue(roles.contains(subject, NewUserRole))
        assertFalse(roles.contains(subject, UserRole))
    }

    /** A missing account is unhandled. */
    @Test
    fun missingUserIsRejected() = runTest {
        val handler = createHandler(FakeUsersRepo(), FakeRolesRepo())

        assertFalse(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
    }

    /** Existing pending accounts become approved and remain approved on repeated opens. */
    @Test
    fun existingUserIsPromotedIdempotently() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertTrue(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, UserRole))
        assertFalse(roles.contains(subject, NewUserRole))
    }

    /** A link sent to an earlier address cannot approve an account after the address changes. */
    @Test
    fun changedEmailRejectsStaleVerificationLink() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val changed = user.copy(email = Email("changed@example.com"))
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to changed)), roles)

        assertFalse(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertFalse(roles.contains(subject, UserRole))
    }

    /** Clearing the stored address leaves the pending account unapproved. */
    @Test
    fun clearedEmailRejectsVerificationLink() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val cleared = user.copy(email = null)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to cleared)), roles)

        assertFalse(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id, user.email)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertFalse(roles.contains(subject, UserRole))
    }

    /** Persisted user-id-only payloads decode compatibly but fail closed at handling time. */
    @Test
    fun legacyPayloadWithoutEmailIsRejected() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = createHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertFalse(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id)))
        assertTrue(roles.contains(subject, NewUserRole))
        assertFalse(roles.contains(subject, UserRole))
    }
}
