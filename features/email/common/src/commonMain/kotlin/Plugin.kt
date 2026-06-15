package dev.inmo.wishlist.features.email.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common (platform-agnostic) startup plugin for the email feature.
 *
 * Currently a no-op placeholder: the `Email` value class and request DTOs need no DI registration.
 * Kept so platform plugins can uniformly delegate to a single common plugin.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {

    }
}