package dev.inmo.wishlist.features.ui.adminPanel

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListViewModel
import dev.inmo.wishlist.features.ui.adminPanel.ui.DefaultAdminPanelModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the admin panel UI feature.
 *
 * Registers in Koin:
 * - Polymorphic serializers for all eight [ViewConfig] subclasses
 * - Koin factories for all eight ViewModels
 * - [AdminPanelModel] singleton backed by [AdminFeature]
 *
 * Platform-specific plugins delegate to this object and register NavigationNodeFactory entries.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, AdminPanelViewConfig::class, AdminPanelViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminPanelViewConfig::class, AdminPanelViewConfig.serializer())
                polymorphic(Any::class, AdminUsersListViewConfig::class, AdminUsersListViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminUsersListViewConfig::class, AdminUsersListViewConfig.serializer())
                polymorphic(Any::class, AdminUserViewConfig::class, AdminUserViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminUserViewConfig::class, AdminUserViewConfig.serializer())
                polymorphic(Any::class, AdminUserEditViewConfig::class, AdminUserEditViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminUserEditViewConfig::class, AdminUserEditViewConfig.serializer())
                polymorphic(Any::class, AdminWishlistsListViewConfig::class, AdminWishlistsListViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminWishlistsListViewConfig::class, AdminWishlistsListViewConfig.serializer())
                polymorphic(Any::class, AdminWishlistViewConfig::class, AdminWishlistViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminWishlistViewConfig::class, AdminWishlistViewConfig.serializer())
                polymorphic(Any::class, AdminWishlistEditViewConfig::class, AdminWishlistEditViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminWishlistEditViewConfig::class, AdminWishlistEditViewConfig.serializer())
                polymorphic(Any::class, AdminWishlistItemEditViewConfig::class, AdminWishlistItemEditViewConfig.serializer())
                polymorphic(ViewConfig::class, AdminWishlistItemEditViewConfig::class, AdminWishlistItemEditViewConfig.serializer())
            }
        }

        factory { AdminPanelViewModel(it.get(), get(), get()) }
        factory { AdminUsersListViewModel(it.get(), get(), get()) }
        factory { AdminUserViewModel(it.get(), get(), get()) }
        factory { AdminUserEditViewModel(it.get(), get(), get()) }
        factory { AdminWishlistsListViewModel(it.get(), get(), get()) }
        factory { AdminWishlistViewModel(it.get(), get(), get()) }
        factory { AdminWishlistEditViewModel(it.get(), get(), get()) }
        factory { AdminWishlistItemEditViewModel(it.get(), get(), get()) }

        single<AdminPanelModel> {
            DefaultAdminPanelModel(
                admin = get(),
                email = get(),
                credentialsStorage = get(),
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
