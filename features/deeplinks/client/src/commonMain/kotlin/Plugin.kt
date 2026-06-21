package dev.inmo.wishlist.features.deeplinks.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Client-common plugin for the deeplinks feature.
 * The deeplinks feature is server-only; this plugin is a scaffold stub with no client logic.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
