package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.meStateFlow
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.client.WishlistsFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature
import dev.inmo.wishlist.features.ui.wishlist.ui.BookingConfigsProvider
import dev.inmo.wishlist.features.ui.wishlist.ui.DefaultWishlistsModel
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistAdditionalConfigsProvider
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemCopyViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemCopyViewModel
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
 * - [WishlistsModel] singleton backed by [WishlistsFeature], [WishlistsItemsFeature] and the
 *   authenticated-user ("me") state flow
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
                polymorphic(Any::class, UserWishlistsViewConfig::class, UserWishlistsViewConfig.serializer())
                polymorphic(ViewConfig::class, UserWishlistsViewConfig::class, UserWishlistsViewConfig.serializer())
                polymorphic(Any::class, WishlistItemCopyViewConfig::class, WishlistItemCopyViewConfig.serializer())
                polymorphic(ViewConfig::class, WishlistItemCopyViewConfig::class, WishlistItemCopyViewConfig.serializer())
            }
        }

        factory { WishlistsListViewModel(it.get(), get(), get()) }
        factory { WishlistViewModel(it.get(), get(), get()) }
        factory { WishlistEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemViewModel(it.get(), get(), get(), getAllDistinct()) }
        factory { UserWishlistsViewModel(it.get(), get(), get()) }
        factory { WishlistItemCopyViewModel(it.get(), get(), get()) }

        singleWithRandomQualifier<WishlistAdditionalConfigsProvider> { BookingConfigsProvider() }

        single<WishlistsModel> {
            DefaultWishlistsModel(
                wishlistsFeature = get(),
                itemsFeature = get(),
                copyFeature = get(),
                meState = meStateFlow,
                filesService = get(),
                usersFeature = get(),
                currencyService = get(),
                viewModeStorage = get(),
                scope = get(),
                credentialsStorage = get(),
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
