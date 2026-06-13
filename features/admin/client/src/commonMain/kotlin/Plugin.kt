package dev.inmo.wishlist.features.admin.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorUsersManagementFeature(get()) }
        single<UsersManagementFeature> { get<KtorUsersManagementFeature>() }

        single { KtorAdminWishlistsFeature(get()) }
        single<AdminWishlistsFeature> { get<KtorAdminWishlistsFeature>() }

        single { KtorAdminWishlistItemsFeature(get()) }
        single<AdminWishlistItemsFeature> { get<KtorAdminWishlistItemsFeature>() }

        single { KtorAdminFeature(get<UsersManagementFeature>(), get<AdminWishlistsFeature>(), get<AdminWishlistItemsFeature>()) }
        single<AdminFeature> { get<KtorAdminFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
