package dev.inmo.wishlist.features.email.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common client startup plugin for the email feature.
 *
 * Registers the [KtorEmailFeature] and binds it as [EmailFeature] using the shared `HttpClient`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorEmailFeature(get()) }
        single<EmailFeature> { get<KtorEmailFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
