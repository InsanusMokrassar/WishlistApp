package dev.inmo.wishlist.features.email.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/** JS-specific startup plugin for the email common module. Delegates to the shared [Plugin]. */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
