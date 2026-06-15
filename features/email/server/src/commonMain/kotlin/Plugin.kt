package dev.inmo.wishlist.features.email.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.services.JavaMailEmailService
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Server startup plugin for the email feature.
 *
 * Registers into the shared DI graph:
 * - [EmailConfig] decoded from the root server config JSON (provides the SMTP block).
 * - [JavaMailEmailService] bound as the [EmailFeature]; disabled (no-op) when no SMTP config is present.
 * - [EmailRoutingsConfigurator] exposing the root-only test-send and self-service set-email endpoints.
 *
 * Depends on [UsersRepo] (from `users.common`) being in the graph for root verification and storing the
 * caller's e-mail; this is satisfied because `auth.server` wires `users.common` into the server graph.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config) }

        single { JavaMailEmailService(get<EmailConfig>().smtp) }
        single<EmailFeature> { get<JavaMailEmailService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(
                emailFeature = get(),
                usersRepo = get<UsersRepo>()
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
