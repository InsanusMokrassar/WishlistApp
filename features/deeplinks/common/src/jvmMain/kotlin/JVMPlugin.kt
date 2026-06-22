package dev.inmo.wishlist.features.deeplinks.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.common.repo.ExposedDeepLinksRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM startup plugin for the deeplinks common module.
 *
 * Registers the Exposed-backed [DeepLinksRepo]. The `Database` comes from `features/common/server`
 * and the `Json` from `features/common/common`, so this plugin must run after the common server
 * plugin (the deeplinks server JVM plugin delegates here first to guarantee that order).
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (Plugin) { setupDI(config) }
        single<DeepLinksRepo> { ExposedDeepLinksRepo(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}