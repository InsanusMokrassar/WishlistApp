package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS platform plugin for the deeplinks common module. Delegates DI setup and startup to [Plugin].
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
