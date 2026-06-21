package dev.inmo.wishlist.features.email.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.email.common.EmailFeature
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.services.SmtpEmailService
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the email server feature.
 *
 * Registers in the shared DI graph:
 * - [EmailConfig] decoded from the root server config JSON using the config-slice pattern.
 * - [SmtpEmailService] as both the concrete service and the [EmailFeature] binding.
 * - [EmailRoutingsConfigurator] registered with a random qualifier so Ktor picks it up automatically.
 *
 * The email server module targets JVM only (`mppJavaProject`), so Jakarta Mail references are safe
 * in this `commonMain` source set — this is the same approach used by `currency/server` with OkHttp.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config) }
        single { SmtpEmailService(get<EmailConfig>()) }
        single<EmailFeature> { get<SmtpEmailService>() }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(get(), get<UsersRepo>())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
