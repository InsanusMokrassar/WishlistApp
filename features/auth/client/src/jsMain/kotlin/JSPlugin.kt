package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.utils.PasswordChangeCompletionUrl
import dev.inmo.wishlist.features.auth.common.Constants
import kotlinx.browser.window
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.auth.common.JSPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<ServerUrlStorage> { LocalStorageServerUrlStorage() }
        single<AuthCredentialsStorage> { LocalStorageAuthCredentialsStorage(get()) }
        single {
            PasswordChangeCompletionUrl(
                "${window.location.origin}/api/${Constants.prefixPathPart}/${Constants.completePasswordChangePathPart}",
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.auth.common.JSPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
