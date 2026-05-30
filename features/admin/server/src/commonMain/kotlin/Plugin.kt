package dev.inmo.wishlist.features.admin.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.server.configurators.AdminRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            UsersManagementFeature(
                usersRepo = get<UsersRepo>(),
                authService = get<AuthFeatureService>(),
                wishlistRepo = get<WishlistRepo>(),
                wishlistItemRepo = get<WishlistItemRepo>()
            )
        }
        single { AdminFeature(get()) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            AdminRoutingsConfigurator(
                adminFeature = get(),
                usersRepo = get<UsersRepo>(),
                wishlistService = get<WishlistService>(),
                wishlistRepo = get<WishlistRepo>(),
                wishlistItemRepo = get<WishlistItemRepo>()
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
