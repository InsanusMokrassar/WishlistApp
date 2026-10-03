package dev.inmo.wishlist.features.email.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.auth.server.Plugin as AuthServerPlugin
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.auth.server.configurators.AuthRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.BearerAuthenticationConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.PasswordChangeRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.common.Plugin as CommonPlugin
import dev.inmo.wishlist.features.common.server.configurators.ApplicationAuthenticationConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.Plugin as DeepLinksServerPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.email.server.services.EmailPasswordChangeDeepLinkHandler
import dev.inmo.wishlist.features.email.server.services.EmailPasswordChangeService
import dev.inmo.wishlist.features.email.server.services.EmailRegistrationInviteSender
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.email.server.services.EmailVerificationDeepLinkHandler
import dev.inmo.wishlist.features.email.server.services.FakeRolesFeature
import dev.inmo.wishlist.features.email.server.services.FakeRolesRepo
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangeDeepLinksRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangePasswordsRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangeRoleAuthorization
import dev.inmo.wishlist.features.email.server.services.SmtpEmailService
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.koin.core.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Controlled external dependencies supplied to one otherwise production password-change Koin graph.
 *
 * @param application Isolated Koin application hosting actual feature plugin definitions.
 * @param users External users persistence double consumed by production Auth and Email services.
 * @param passwords External password persistence double consumed by production Auth service.
 * @param linksRepo External deeplink persistence double consumed by production deeplink service.
 */
private class PasswordChangePluginGraph(
    /** Isolated Koin application hosting actual feature plugin definitions. */
    val application: KoinApplication,
    /** External users persistence double consumed by production Auth and Email services. */
    val users: FakeUsersRepo,
    /** External password persistence double consumed by production Auth service. */
    val passwords: PasswordChangePasswordsRepo,
    /** External deeplink persistence double consumed by production deeplink service. */
    val linksRepo: PasswordChangeDeepLinksRepo,
) {
    /** Closes the isolated Koin graph and releases registered singleton definitions. */
    fun close() {
        application.close()
    }
}

/** Verifies the complete production password-change plugin graph and persisted payload contract. */
class PasswordChangePluginTest {
    /** Approved account used by real Auth and Email services in every graph. */
    private val user = RegisteredUser(
        id = UserId(7L),
        username = Username("owner"),
        email = Email("owner@example.com"),
        emailApproved = true,
    )

    /** Creates a root configuration with or without the Email SMTP block. */
    private fun config(smtpEnabled: Boolean): JsonObject = buildJsonObject {
        put("publicHttpOrigin", "https://wishlist.example")
        if (smtpEnabled) {
            putJsonObject("email") {
                putJsonObject("smtp") {
                    put("host", "smtp.example.com")
                    put("from", "noreply@example.com")
                }
            }
        }
    }

    /** Builds a graph from actual Common, Auth, deeplinks, and optionally Email server plugins. */
    private fun graph(smtpEnabled: Boolean, includeEmailPlugin: Boolean = true): PasswordChangePluginGraph {
        val users = FakeUsersRepo(mapOf(user.id to user))
        val passwords = PasswordChangePasswordsRepo()
        val linksRepo = PasswordChangeDeepLinksRepo()
        val application = KoinApplication.init()
        application.modules(module {
            single<UsersRepo> { users }
            single<ReadUsersRepo> { users }
            single<WriteUsersRepo> { users }
            single<PasswordsRepo> { passwords }
            single<DeepLinksRepo> { linksRepo }
            single<RolesRepo> { FakeRolesRepo() }
            single<RolesFeature> { FakeRolesFeature() }
            single<UserRoleAuthorization> { PasswordChangeRoleAuthorization() }
            with(CommonPlugin) { setupDI(config(smtpEnabled)) }
            with(AuthServerPlugin) { setupDI(config(smtpEnabled)) }
            with(DeepLinksServerPlugin) { setupDI(config(smtpEnabled)) }
            if (includeEmailPlugin) {
                with(Plugin) { setupDI(config(smtpEnabled)) }
            }
        })
        return PasswordChangePluginGraph(application, users, passwords, linksRepo)
    }

    /** Resolves actual Auth and deeplink services in the requested order to expose construction cycles. */
    private fun resolveGraphServices(graph: PasswordChangePluginGraph, authFirst: Boolean): Pair<AuthFeatureService, DeepLinksService> {
        val koin = graph.application.koin
        return if (authFirst) {
            koin.get<AuthFeatureService>() to koin.get<DeepLinksService>()
        } else {
            val links = koin.get<DeepLinksService>()
            koin.get<AuthFeatureService>() to links
        }
    }

    /** Removes one payload property while retaining production array-polymorphic structure. */
    private fun withoutPayloadField(encoded: JsonElement, field: String): JsonElement {
        val root = encoded.jsonObject
        val polymorphicValue = root.getValue("value").jsonArray
        val payload = polymorphicValue[1].jsonObject
        return buildJsonObject {
            root.forEach { (key, value) ->
                if (key == "value") {
                    put(key, buildJsonArray {
                        add(polymorphicValue[0])
                        add(buildJsonObject {
                            payload.forEach { (payloadKey, payloadValue) ->
                                if (payloadKey != field) put(payloadKey, payloadValue)
                            }
                        })
                    })
                } else {
                    put(key, value)
                }
            }
        }
    }

    /** Proves production graph shape, deferred handler resolution, and configuration contributions. */
    private suspend fun assertProductionGraph(smtpEnabled: Boolean, authFirst: Boolean) {
        val graph = graph(smtpEnabled)
        try {
            val (auth, links) = resolveGraphServices(graph, authFirst)
            auth.setPassword(user.id, dev.inmo.wishlist.features.auth.common.models.Password("old-password"))
            graph.passwords.resetIssuedPasswordWriteCount()
            val koin = graph.application.koin
            val coordinator = koin.get<EmailVerificationAccountCoordinator>()
            assertEquals(1, koin.getAll<EmailVerificationAccountCoordinator>().size)
            assertSame(coordinator, koin.get<EmailVerificationAccountCoordinator>())
            assertSame(koin.get<EmailPasswordChangeService>(), koin.get<ServerPasswordChangeFeature>())

            val handlers = koin.getAllDistinct<DeepLinkHandler>()
            assertEquals(1, handlers.filterIsInstance<EmailVerificationDeepLinkHandler>().size)
            assertEquals(1, handlers.filterIsInstance<EmailPasswordChangeDeepLinkHandler>().size)
            assertSame(links, koin.get<DeepLinksService>())

            val credentialState = auth.passwordChangeState(user.id)!!
            val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")
            graph.linksRepo.set(
                approvalId,
                DeepLinkHandlerInfo(
                    EmailPasswordChange.handlerId,
                    EmailPasswordChangePayload(
                        user.id,
                        user.email!!,
                        System.currentTimeMillis() + 60_000L,
                        credentialState,
                    ),
                ),
            )
            assertEquals(
                dev.inmo.wishlist.features.deeplinks.common.models.HandleResult.Handled.Redirect(
                    "/password-change/${user.id.long}/${approvalId.string}",
                ),
                links.handle(approvalId),
            )

            val routes = koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()
            assertEquals(1, routes.filterIsInstance<AuthRoutingsConfigurator>().size)
            assertEquals(1, routes.filterIsInstance<PasswordChangeRoutingsConfigurator>().size)
            assertEquals(1, routes.filterIsInstance<DeepLinksRoutingConfigurator>().size)
            assertEquals(1, routes.filterIsInstance<EmailRoutingsConfigurator>().size)
            assertEquals(1, koin.getAllDistinct<ApplicationAuthenticationConfigurator.Element>()
                .filterIsInstance<BearerAuthenticationConfigurator>().size)
            assertSame(koin.get<EmailRegistrationInviteSender>(), koin.get<dev.inmo.wishlist.features.auth.server.RegistrationEmailSender>())
            if (smtpEnabled) {
                assertIs<SmtpEmailService>(koin.get<SmtpEmailService>())
                assertIs<EmailFeatureService>(koin.get<EmailFeature>())
            } else {
                assertNull(koin.getOrNull<SmtpEmailService>())
                assertIs<DisabledEmailFeature>(koin.get<EmailFeature>())
            }
        } finally {
            graph.close()
        }
    }

    /** Production graphs resolve Auth-first and deeplinks-first in both SMTP graph shapes. */
    /** Verifies production plugin order resolves every password-change binding and SMTP shape. */
    @Test
    fun productionPluginsResolveInEveryRequiredOrderAndSmtpShape() = kotlinx.coroutines.test.runTest {
        assertProductionGraph(smtpEnabled = false, authFirst = true)
        assertProductionGraph(smtpEnabled = false, authFirst = false)
        assertProductionGraph(smtpEnabled = true, authFirst = true)
        assertProductionGraph(smtpEnabled = true, authFirst = false)
    }

    /** Aggregated production Json persists the stable password payload name and keeps verification payload support. */
    /** Verifies production JSON round-trips payloads and rejects missing fields. */
    @Test
    fun productionJsonRoundTripsPasswordPayloadAndRejectsMissingRequiredFields() {
        val graph = graph(smtpEnabled = false)
        try {
            val json = graph.application.koin.get<Json>()
            val passwordInfo = DeepLinkHandlerInfo(
                EmailPasswordChange.handlerId,
                EmailPasswordChangePayload(user.id, user.email!!, 2_000L, "credential-state"),
            )
            val encoded = json.encodeToString(passwordInfo)
            assertTrue(encoded.contains("email.password_change.v1"))
            assertEquals(passwordInfo, json.decodeFromString<DeepLinkHandlerInfo>(encoded))
            val encodedElement = json.parseToJsonElement(encoded)
            listOf("userId", "approvedEmail", "expiresAtEpochMillis", "credentialState").forEach { field ->
                assertFailsWith<SerializationException> {
                    json.decodeFromString<DeepLinkHandlerInfo>(
                        json.encodeToString(JsonElement.serializer(), withoutPayloadField(encodedElement, field)),
                    )
                }
            }
            val verificationInfo = DeepLinkHandlerInfo(
                DeepLinkHandlerId("email.verification"),
                EmailVerificationPayload(user.id, user.email),
            )
            assertEquals(verificationInfo, json.decodeFromString<DeepLinkHandlerInfo>(json.encodeToString(verificationInfo)))
        } finally {
            graph.close()
        }
    }

    /** Auth remains independently constructible when Email's optional password-change port is absent. */
    /** Verifies absent Email infrastructure leaves the optional port unset. */
    @Test
    fun absentEmailPluginLeavesOptionalPasswordChangePortUnset() {
        val graph = graph(smtpEnabled = false, includeEmailPlugin = false)
        try {
            val koin = graph.application.koin
            assertNull(koin.getOrNull<ServerPasswordChangeFeature>())
            assertEquals(1, koin.getAllDistinct<ApplicationRoutingConfigurator.Element>()
                .filterIsInstance<PasswordChangeRoutingsConfigurator>().size)
        } finally {
            graph.close()
        }
    }
}
