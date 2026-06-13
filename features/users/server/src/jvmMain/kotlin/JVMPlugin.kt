package dev.inmo.wishlist.features.users.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.users.server.configurators.UsersRoutingsConfigurator
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM-specific startup plugin for the public users server feature.
 *
 * Delegates to the common [Plugin] for service bindings and registers the
 * routing element so Ktor installs `/users/getAll`.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.users.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            UsersRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.users.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
