package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
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
    private val user = RegisteredUser(UserId(7L), Username("alice"))
    private val deeplinkId = DeepLinkId("verification-7")

    /** Wrong payload type is unhandled. */
    @Test
    fun wrongPayloadTypeIsRejected() = runTest {
        val handler = EmailVerificationDeepLinkHandler(FakeUsersRepo(mapOf(user.id to user)), FakeRolesRepo())

        assertFalse(handler.tryHandle(deeplinkId, "wrong"))
    }

    /** A missing account is unhandled. */
    @Test
    fun missingUserIsRejected() = runTest {
        val handler = EmailVerificationDeepLinkHandler(FakeUsersRepo(), FakeRolesRepo())

        assertFalse(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id)))
    }

    /** Existing pending accounts become approved and remain approved on repeated opens. */
    @Test
    fun existingUserIsPromotedIdempotently() = runTest {
        val roles = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        roles.includeDirect(subject, NewUserRole)
        val handler = EmailVerificationDeepLinkHandler(FakeUsersRepo(mapOf(user.id to user)), roles)

        assertTrue(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id)))
        assertTrue(handler.tryHandle(deeplinkId, EmailVerificationPayload(user.id)))
        assertTrue(roles.contains(subject, UserRole))
        assertFalse(roles.contains(subject, NewUserRole))
    }
}
