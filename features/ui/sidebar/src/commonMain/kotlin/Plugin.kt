package dev.inmo.wishlist.features.ui.sidebar

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarModel
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewConfig
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsModel
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the sidebar UI feature.
 *
 * Registers the polymorphic serializer for [SidebarViewConfig], the [SidebarViewModel] factory, and
 * a [SidebarModel] singleton that composes the already-registered [WishlistsModel] and [BookingModel]
 * singletons (both resolved lazily so this plugin needs no cross-feature `setupDI` delegation).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, SidebarViewConfig::class, SidebarViewConfig.serializer())
                polymorphic(ViewConfig::class, SidebarViewConfig::class, SidebarViewConfig.serializer())
            }
        }

        single<SidebarModel> {
            val wishlistsModel = get<WishlistsModel>()
            val bookingModel = get<BookingModel>()
            object : SidebarModel {
                override val currentUserIdFlow: StateFlow<UserId?> = wishlistsModel.currentUserIdFlow
                override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> = wishlistsModel.getMyWishlists()
                override suspend fun getReservedCount(): Int = bookingModel.myPresentsBooks().size
                override suspend fun getUserName(userId: UserId): String? = wishlistsModel.getUserName(userId)
            }
        }

        factory { SidebarViewModel(node = it.get(), model = get(), interactor = get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
