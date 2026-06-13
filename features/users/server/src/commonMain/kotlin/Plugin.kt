package dev.inmo.wishlist.features.users.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.server.services.UsersService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (platform-agnostic) startup plugin for the public users server feature.
 *
 * Registers [UsersService] and exposes it as the [UsersFeature] binding.
 * Routes are installed by [dev.inmo.wishlist.features.users.server.JVMPlugin] on JVM.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { UsersService(get<UsersRepo>()) }
        single<UsersFeature> { get<UsersService>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
