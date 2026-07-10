package dev.inmo.wishlist.features.email.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common (platform-agnostic) startup plugin for the email feature.
 *
 * No DI registrations are required for the shared common module — it only provides value types
 * and constants. The [EmailFeature] interface has platform-specific variants in the client and
 * server modules respectively. Platform plugins delegate to this object so that the delegation
 * chain stays consistent across all source sets.
 */
object Plugin : StartPlugin {
    /** No shared DI registrations required for the common email module. */
    override fun Module.setupDI(config: JsonObject) {
    }
}
