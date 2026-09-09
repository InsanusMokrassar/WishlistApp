package dev.inmo.wishlist.features.email.server

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.auth.server.RegistrationEmailSender
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.server.models.Config as ServerConfig
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.email.server.services.SmtpEmailService
import dev.inmo.wishlist.features.email.server.services.EmailRegistrationInviteSender
import dev.inmo.wishlist.features.email.server.services.EmailVerificationDeepLinkHandler
import dev.inmo.wishlist.features.email.server.services.EmailPasswordChangeDeepLinkHandler
import dev.inmo.wishlist.features.email.server.services.EmailPasswordChangeService
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.common.models.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.utils.singleRequirement
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the email server feature.
 *
 * Registers in the shared DI graph:
 * - [EmailConfig], [SmtpEmailService], and the [SmtpEmailService]-backed [EmailsService] binding —
 *   ALL THREE registered together, and ONLY when [emailConfigElementOrNull] returns non-null (a
 *   non-null `"email"` JSON object is present at `config["email"]` in the root server config). When
 *   absent (or explicitly JSON `null`), none of the three are registered — "email delivery disabled"
 *   is a DI-graph-shape fact rather than a runtime value check.
 * - [EmailVerificationAccountCoordinator], registered exactly once and unconditionally so account
 *   mutation and verification share one atomicity boundary in both SMTP graph shapes.
 * - [EmailFeature], always registered unconditionally via an inline `single<EmailFeature>` block
 *   that resolves `getOrNull<EmailsService>()`: present → [EmailFeatureService]; absent →
 *   [DisabledEmailFeature] (both wrapping the shared [EmailVerificationAccountCoordinator], so
 *   per-user email storage and verification atomicity do not depend on SMTP configuration).
 * - [EmailRoutingsConfigurator], registered with a random qualifier so Ktor picks it up automatically.
 *
 * [emailConfigElementOrNull] is extracted as a pure, Koin-free top-level function so the `"email"`
 * config-key-presence decision remains directly unit testable. Dedicated Koin graph tests also prove
 * that both [EmailFeature] realizations and [EmailVerificationDeepLinkHandler] resolve the same single
 * coordinator instance.
 *
 * The email server module targets JVM only (`mppJavaProject`), so Jakarta Mail references are safe
 * in this `commonMain` source set — the same approach used by `currency/server` with OkHttp.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        val emailConfigElement = emailConfigElementOrNull(config)
        val serverConfig = Json { ignoreUnknownKeys = true }
            .decodeFromJsonElement(ServerConfig.serializer(), config)
        if (emailConfigElement != null) {
            single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), emailConfigElement) }
            single { SmtpEmailService(get<EmailConfig>()) }
            single<EmailsService> { get<SmtpEmailService>() }
        }
        single {
            EmailVerificationAccountCoordinator(
                usersRepo = get<UsersRepo>(),
                rolesRepo = get<RolesRepo>(),
            )
        }
        single<EmailFeature> {
            val accountCoordinator = get<EmailVerificationAccountCoordinator>()
            getOrNull<EmailsService>()?.let {
                EmailFeatureService(it, accountCoordinator, get<RolesFeature>(), get<EmailRegistrationInviteSender>())
            } ?: DisabledEmailFeature(accountCoordinator)
        }
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, EmailVerificationPayload::class, EmailVerificationPayload.serializer())
                polymorphic(Any::class, EmailPasswordChangePayload::class, EmailPasswordChangePayload.serializer())
            }
        }
        singleWithRandomQualifier<DeepLinkHandler> {
            EmailVerificationDeepLinkHandler(
                accountCoordinator = get<EmailVerificationAccountCoordinator>(),
                emailsService = getOrNull<EmailsService>(),
            )
        }
        singleWithRandomQualifier<DeepLinkHandler> {
            EmailPasswordChangeDeepLinkHandler { get<EmailPasswordChangeService>() }
        }
        single {
            EmailRegistrationInviteSender(
                emailsService = getOrNull(),
                deepLinksService = getOrNull<DeepLinksService>(),
                publicHttpOrigin = serverConfig.publicHttpOrigin,
            )
        }
        single<RegistrationEmailSender> { get<EmailRegistrationInviteSender>() }
        single {
            EmailPasswordChangeService(
                emailsService = getOrNull(),
                deepLinksService = getOrNull(),
                accountCoordinator = get(),
                authFeatureService = get<AuthFeatureService>(),
                publicHttpOrigin = serverConfig.publicHttpOrigin,
            )
        }
        single<ServerPasswordChangeFeature> { get<EmailPasswordChangeService>() }
        singleRequirement {
            FeatureRolesRegistry.Requirement(EmailConstants.sendTestFunctionalityId, SuperAdminRole)
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}

/**
 * Returns the nested `"email"` JSON object from [config], or `null` when the key is absent or
 * explicitly JSON `null` — both are treated as "email feature disabled" (see [EmailConfig] and
 * [Plugin]). Pure and Koin-free so it is directly unit-testable (see `PluginTest`).
 *
 * @param config Root server config JSON object.
 * @return The `"email"` [JsonElement] to decode [EmailConfig] from, or `null` to skip registration.
 */
internal fun emailConfigElementOrNull(config: JsonObject): JsonElement? =
    config["email"]?.takeUnless { it == JsonNull }
