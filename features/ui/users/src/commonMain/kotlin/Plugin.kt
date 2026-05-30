package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.ui.users.ui.UsersListModel
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the users list UI feature.
 *
 * Registers polymorphic serializer for [UsersListViewConfig], the [UsersListViewModel]
 * factory, and the [UsersListModel] singleton wrapping the public [UsersFeature].
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, UsersListViewConfig::class, UsersListViewConfig.serializer())
                polymorphic(ViewConfig::class, UsersListViewConfig::class, UsersListViewConfig.serializer())
            }
        }
        factory { UsersListViewModel(node = it.get(), model = get(), interactor = get()) }
        single<UsersListModel> {
            val feature = get<UsersFeature>()
            val authFeature = get<ClientAuthFeature>()
            val adminFeature = get<AdminFeature>()
            object : UsersListModel {
                override suspend fun getAllUsers() = feature.getAll()

                override suspend fun isCurrentUserRoot(): Boolean =
                    authFeature.getMe()?.username?.string == "root"

                override suspend fun deleteUser(id: UserId): Boolean =
                    adminFeature.usersManagement.delete(id)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
