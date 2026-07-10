package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.meStateFlow
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common startup plugin for the `simpleRoles` client feature.
 *
 * Registers [KtorSimpleRolesFeature] (HTTP-only) and [CacheSimpleRolesFeature] (the bound
 * [SimpleRolesFeature] implementation — caches the boolean answer, refreshed off
 * `features/auth/client`'s `meStateFlow`).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorSimpleRolesFeature(get()) }
        single { CacheSimpleRolesFeature(get(), meStateFlow, get()) }
        single<SimpleRolesFeature> { get<CacheSimpleRolesFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
