package dev.inmo.wishlist.features.admin.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {}
}
