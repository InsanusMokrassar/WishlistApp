package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common plugin for the deeplinks feature.
 * Contains no DI bindings — shared constants and models are in this module only.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
    }
}
