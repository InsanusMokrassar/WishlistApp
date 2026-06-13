package dev.inmo.wishlist.features.users.common

import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (Plugin) { setupDI(config) }

        single { ExposedUsersRepo(get()) }
        singleWithBinds<UsersRepo> {
            CacheUsersRepo(originalRepo = get<ExposedUsersRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}