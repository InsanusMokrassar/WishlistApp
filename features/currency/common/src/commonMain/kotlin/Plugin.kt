package dev.inmo.wishlist.features.currency.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common (platform-agnostic) startup plugin for the currency feature.
 *
 * Holds no DI registrations of its own — shared models and pure utilities require no wiring. Exists so
 * the platform plugins have a single parent to delegate to.
 */
object Plugin : StartPlugin {
    /** No shared DI registrations are required for the common currency module. */
    override fun Module.setupDI(config: JsonObject) {
    }
}