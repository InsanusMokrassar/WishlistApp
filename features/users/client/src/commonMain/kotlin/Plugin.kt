package dev.inmo.wishlist.features.users.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (platform-agnostic) startup plugin for the public users client feature.
 *
 * Registers [KtorUsersFeature] and exposes it as the [UsersFeature] binding.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorUsersFeature(get()) }
        single<UsersFeature> { get<KtorUsersFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
