package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.client.WishlistsFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the wishlist UI feature.
 *
 * Registers in Koin:
 * - Polymorphic serializers for all four [ViewConfig] subclasses
 * - Koin factories for all four ViewModels
 * - [WishlistsModel] singleton backed by [WishlistsFeature], [WishlistsItemsFeature], [ClientAuthFeature]
 *
 * Platform-specific plugins (JSPlugin, JVMPlugin, AndroidPlugin) delegate to this object
 * and register [dev.inmo.navigation.core.NavigationNodeFactory] entries for each View.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, WishlistsListViewConfig::class, WishlistsListViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistsListViewConfig::class, WishlistsListViewConfig.serializer())
                polymorphic(Any::class, WishlistViewConfig::class, WishlistViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistViewConfig::class, WishlistViewConfig.serializer())
                polymorphic(Any::class, WishlistEditViewConfig::class, WishlistEditViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistEditViewConfig::class, WishlistEditViewConfig.serializer())
                polymorphic(Any::class, WishlistItemEditViewConfig::class, WishlistItemEditViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistItemEditViewConfig::class, WishlistItemEditViewConfig.serializer())
                polymorphic(Any::class, WishlistItemViewConfig::class, WishlistItemViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistItemViewConfig::class, WishlistItemViewConfig.serializer())
            }
        }

        factory { WishlistsListViewModel(it.get(), get(), get()) }
        factory { WishlistViewModel(it.get(), get(), get()) }
        factory { WishlistEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemViewModel(it.get(), get(), get()) }

        single<WishlistsModel> {
            val wishlistsFeature = get<WishlistsFeature>()
            val itemsFeature = get<WishlistsItemsFeature>()
            val authFeature = get<ClientAuthFeature>()
            object : WishlistsModel {
                override suspend fun getMyWishlists(): List<RegisteredWishlist> =
                    wishlistsFeature.getMyWishlists()

                override suspend fun getUserWishlists(userId: UserId): List<RegisteredWishlist> =
                    wishlistsFeature.getByUserId(userId)

                override suspend fun getWishlist(id: WishlistId): RegisteredWishlist? =
                    wishlistsFeature.getById(id)

                override suspend fun getWishlistItems(wishlistId: WishlistId): List<RegisteredWishlistItem> =
                    itemsFeature.getByWishlistId(wishlistId)

                override suspend fun createWishlist(title: String): RegisteredWishlist? =
                    wishlistsFeature.create(NewWishlistInFeature(title))

                override suspend fun updateWishlist(id: WishlistId, title: String): Boolean =
                    wishlistsFeature.update(id, NewWishlistInFeature(title))

                override suspend fun deleteWishlist(id: WishlistId): Boolean =
                    wishlistsFeature.delete(id)

                override suspend fun createWishlistItem(item: NewWishlistItem): RegisteredWishlistItem? =
                    itemsFeature.create(item)

                override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean =
                    itemsFeature.update(id, item)

                override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean =
                    itemsFeature.delete(id)

                override suspend fun getCurrentUserId(): UserId? =
                    authFeature.getMe()?.id
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
