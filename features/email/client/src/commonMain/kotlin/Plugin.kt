package dev.inmo.wishlist.features.email.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.email.common.EmailFeature
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common client startup plugin for the email feature.
 *
 * Registers [KtorEmailFeature] (HTTP transport only) and binds it as the public [EmailFeature].
 * The shared [io.ktor.client.HttpClient] is resolved from `features/common/client`.
 *
 * No service wrapper is needed here — there is no client-side state or memoization for this feature.
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
