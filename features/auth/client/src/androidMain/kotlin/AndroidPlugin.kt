package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.auth.common.AndroidPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<ServerUrlStorage> { SharedPreferencesServerUrlStorage(get()) }
        single<AuthCredentialsStorage> { SharedPreferencesAuthCredentialsStorage(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.auth.common.AndroidPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}