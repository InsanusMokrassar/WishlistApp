package dev.inmo.wishlist.features.roles.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common startup plugin for the `roles` client feature. Registers [KtorRolesFeature] (HTTP-only) and
 * binds it as the public [RolesFeature]. Reactive per-functionality caching is a consumer concern
 * (built in the Model layer from `meStateFlow`), so no cache class is registered here.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorRolesFeature(get()) }
        single<RolesFeature> { get<KtorRolesFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
