package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.currency.client.CurrencyService
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.client.UsersFeature
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
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewMode
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewModeStorage
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.BookingConfigsProvider
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistAdditionalConfigsProvider
import kotlinx.coroutines.flow.StateFlow
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
                polymorphic(Any::class, UserWishlistsViewConfig::class, UserWishlistsViewConfig.serializer())
                polymorphic(ViewConfig::class, UserWishlistsViewConfig::class, UserWishlistsViewConfig.serializer())
            }
        }

        factory { WishlistsListViewModel(it.get(), get(), get()) }
        factory { WishlistViewModel(it.get(), get(), get()) }
        factory { WishlistEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemEditViewModel(it.get(), get(), get()) }
        factory { WishlistItemViewModel(it.get(), get(), get(), getAllDistinct()) }
        factory { UserWishlistsViewModel(it.get(), get(), get()) }

        singleWithRandomQualifier<WishlistAdditionalConfigsProvider> { BookingConfigsProvider() }

        single<WishlistsModel> {
            val wishlistsFeature = get<WishlistsFeature>()
            val itemsFeature = get<WishlistsItemsFeature>()
            val authFeature = get<ClientAuthFeature>()
            val filesService = get<FilesClientService>()
            val usersFeature = get<UsersFeature>()
            val currencyService = get<CurrencyService>()
            val viewModeStorage = get<WishlistViewModeStorage>()
            object : WishlistsModel {
                override suspend fun getSavedViewMode(): WishlistViewMode =
                    viewModeStorage.getViewMode() ?: WishlistViewMode.List

                override suspend fun saveViewMode(mode: WishlistViewMode) =
                    viewModeStorage.saveViewMode(mode)

                override val selectedCurrency: StateFlow<CurrencyCode?> = currencyService.selectedCurrency

                override suspend fun isCurrencyEnabled(): Boolean = currencyService.isFeatureEnabled()

                override suspend fun availableCurrencies(): List<CurrencyInfo> = currencyService.getCurrencies()

                override suspend fun currencyRates(): CurrencyRates? = currencyService.getRates()

                override fun selectCurrency(code: CurrencyCode?) = currencyService.select(code)

                override suspend fun getMyWishlists(): List<RegisteredWishlist> =
                    wishlistsFeature.getMyWishlists()

                override suspend fun getUserWishlists(userId: UserId): List<RegisteredWishlist> =
                    wishlistsFeature.getByUserId(userId)

                override suspend fun getWishlist(id: WishlistId): RegisteredWishlist? =
                    wishlistsFeature.getById(id)

                override suspend fun getWishlistItems(wishlistId: WishlistId): List<RegisteredWishlistItem> =
                    itemsFeature.getByWishlistId(wishlistId)

                override suspend fun createWishlist(title: String, defaultPriceUnits: String): RegisteredWishlist? =
                    wishlistsFeature.create(NewWishlistInFeature(title, defaultPriceUnits))

                override suspend fun updateWishlist(id: WishlistId, title: String, defaultPriceUnits: String): Boolean =
                    wishlistsFeature.update(id, NewWishlistInFeature(title, defaultPriceUnits))

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

                override suspend fun getUserName(userId: UserId): String? =
                    usersFeature.getAll().find { it.id == userId }?.username?.string

                override suspend fun uploadImage(file: MPPFile): FileId? =
                    filesService.uploadFile(file)?.id

                override fun imageUrl(id: FileId): String =
                    filesService.fileUrl(id)

                override suspend fun loadImageBytes(id: FileId): ByteArray? =
                    filesService.downloadBytes(id)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
