package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Exercises application-level verification payload serialization and deeplink dispatch. */
class EmailDeepLinkIntegrationTest {
    /** In-memory persistence used to exercise mint-then-handle behavior without PostgreSQL. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /** Existing account fixture used by the verification handler. */
    private val user = RegisteredUser(UserId(11L), Username("alice"), Email("alice@example.com"))

    /** The registered application serializer module round-trips the polymorphic verification value. */
    @Test
    fun applicationJsonRoundTripsVerificationPayload() {
        val json = Json {
            useArrayPolymorphism = true
            serializersModule = SerializersModule {
                polymorphic(
                    Any::class,
                    EmailVerificationPayload::class,
                    EmailVerificationPayload.serializer(),
                )
            }
        }
        val info = DeepLinkHandlerInfo(
            handlerId = EmailVerification.handlerId,
            value = EmailVerificationPayload(user.id, user.email),
        )

        val decoded = json.decodeFromString<DeepLinkHandlerInfo>(json.encodeToString(info))

        assertEquals(EmailVerification.handlerId, decoded.handlerId)
        assertEquals(EmailVerificationPayload(user.id, user.email), decoded.value)
    }

    /** The pre-email payload shape remains decodable with a null address for fail-closed handling. */
    @Test
    fun legacyPayloadDecodesWithNullEmail() {
        val decoded = Json.decodeFromString<EmailVerificationPayload>("{\"userId\":11}")

        assertEquals(EmailVerificationPayload(user.id), decoded)
    }

    /** A minted verification link is dispatched through the handler and leaves exactly UserRole. */
    @Test
    fun mintedVerificationLinkDispatchesToApprovedRoleState() = runTest {
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)
        val handler = EmailVerificationDeepLinkHandler(
            accountCoordinator = EmailVerificationAccountCoordinator(
                usersRepo = FakeUsersRepo(mapOf(user.id to user)),
                rolesRepo = rolesRepo,
            ),
        )
        val service = DeepLinksService(FakeDeepLinksRepo(), listOf(handler))
        val deeplinkId = service.createDeepLink(
            EmailVerification.handlerId,
            EmailVerificationPayload(user.id, user.email),
        )

        assertEquals(
            HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath),
            service.handle(deeplinkId),
        )
        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** Approval persists before a failed grant, and reopening the same link finishes pending cleanup. */
    @Test
    fun sameVerificationLinkRetriesAfterApprovedGrantFailure() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(user.id to user))
        val backingRolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        backingRolesRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FailAfterFirstUserGrant(backingRolesRepo)
        val handler = EmailVerificationDeepLinkHandler(
            accountCoordinator = EmailVerificationAccountCoordinator(usersRepo, rolesRepo),
        )
        val service = DeepLinksService(FakeDeepLinksRepo(), listOf(handler))
        val deeplinkId = service.createDeepLink(
            EmailVerification.handlerId,
            EmailVerificationPayload(user.id, user.email),
        )

        try {
            service.handle(deeplinkId)
            fail("Expected the first role grant to fail after approval")
        } catch (_: IllegalStateException) {
        }

        assertTrue(usersRepo.getById(user.id)?.emailApproved == true)
        assertEquals(setOf(NewUserRole, UserRole), backingRolesRepo.getDirectRoles(subject).toSet())

        assertEquals(
            HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath),
            service.handle(deeplinkId),
        )
        assertEquals(setOf(UserRole), backingRolesRepo.getDirectRoles(subject).toSet())
    }

    /** One-shot fault adapter that models a server write accepted before the caller lost its response. */
    private class FailAfterFirstUserGrant(
        private val delegate: RolesRepo,
    ) : RolesRepo by delegate {
        private var failNextUserGrant = true

        override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
            val changed = delegate.includeDirect(subject, role)
            if (role == UserRole && failNextUserGrant) {
                failNextUserGrant = false
                throw IllegalStateException("grant response lost after mutation")
            }
            return changed
        }
    }
}
