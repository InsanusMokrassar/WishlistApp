package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
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

/** Exercises application-level verification payload serialization and deeplink dispatch. */
class EmailDeepLinkIntegrationTest {
    /** In-memory persistence used to exercise mint-then-handle behavior without PostgreSQL. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /** Existing account fixture used by the verification handler. */
    private val user = RegisteredUser(UserId(11L), Username("alice"))

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
            value = EmailVerificationPayload(user.id),
        )

        val decoded = json.decodeFromString<DeepLinkHandlerInfo>(json.encodeToString(info))

        assertEquals(EmailVerification.handlerId, decoded.handlerId)
        assertEquals(EmailVerificationPayload(user.id), decoded.value)
    }

    /** A minted verification link is dispatched through the handler and leaves exactly UserRole. */
    @Test
    fun mintedVerificationLinkDispatchesToApprovedRoleState() = runTest {
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(user.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)
        val handler = EmailVerificationDeepLinkHandler(
            usersRepo = FakeUsersRepo(mapOf(user.id to user)),
            rolesRepo = rolesRepo,
        )
        val service = DeepLinksService(FakeDeepLinksRepo(), listOf(handler))
        val deeplinkId = service.createDeepLink(
            EmailVerification.handlerId,
            EmailVerificationPayload(user.id),
        )

        assertEquals(HandleResult.Handled, service.handle(deeplinkId))
        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }
}
