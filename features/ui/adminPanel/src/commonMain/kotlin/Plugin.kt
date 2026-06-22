package dev.inmo.wishlist.features.ui.adminPanel

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
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
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
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
            val admin = get<AdminFeature>()
            val credentialsStorage = get<AuthCredentialsStorage>()
            object : AdminPanelModel {
                override val userAuthorisedState = credentialsStorage.userAuthorised

                override suspend fun getAllUsers(): List<RegisteredUser> =
                    admin.usersManagement.getAll()

                override suspend fun getUserById(id: UserId): RegisteredUser? =
                    admin.usersManagement.getById(id)

                override suspend fun createUser(newUser: NewUserWithPassword): RegisteredUser? =
                    admin.usersManagement.create(newUser)

                override suspend fun updateUser(id: UserId, newUser: NewUser): Boolean =
                    admin.usersManagement.update(id, newUser)

                override suspend fun deleteUser(id: UserId): Boolean =
                    admin.usersManagement.delete(id)

                override suspend fun getAllWishlists(): List<RegisteredWishlist> =
                    admin.wishlists.getAll()

                override suspend fun getWishlistsByUser(userId: UserId): List<RegisteredWishlist> =
                    admin.wishlists.getByUserId(userId)

                override suspend fun getWishlistById(id: WishlistId): RegisteredWishlist? =
                    admin.wishlists.getById(id)

                override suspend fun createWishlist(newWishlist: NewWishlist): RegisteredWishlist? =
                    admin.wishlists.create(newWishlist)

                override suspend fun updateWishlist(id: WishlistId, userId: UserId, title: String): Boolean =
                    admin.wishlists.update(id, NewWishlistInFeature(title))

                override suspend fun deleteWishlist(id: WishlistId): Boolean =
                    admin.wishlists.delete(id)

                override suspend fun getItemsByWishlist(wishlistId: WishlistId): List<RegisteredWishlistItem> =
                    admin.wishlistItems.getByWishlistId(wishlistId)

                override suspend fun createWishlistItem(item: NewWishlistItem): RegisteredWishlistItem? =
                    admin.wishlistItems.create(item)

                override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean =
                    admin.wishlistItems.update(id, item)

                override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean =
                    admin.wishlistItems.delete(id)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
