package dev.inmo.wishlist.features.ui.booking

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.booking.client.BookingFeature
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewModel
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewModel
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the booking UI scenario.
 *
 * Registers in Koin:
 * - Polymorphic serializers for [BookingViewConfig] (view A) and [MyPresentsBooksViewConfig] (view B).
 * - Koin factories for [BookingViewModel] and [MyPresentsBooksViewModel].
 * - [BookingModel] singleton backed by [BookingFeature].
 *
 * Platform-specific plugins register the [dev.inmo.navigation.core.NavigationNodeFactory] entries.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, BookingViewConfig::class, BookingViewConfig.serializer())
                polymorphic(ViewConfig::class, BookingViewConfig::class, BookingViewConfig.serializer())
                polymorphic(Any::class, MyPresentsBooksViewConfig::class, MyPresentsBooksViewConfig.serializer())
                polymorphic(ViewConfig::class, MyPresentsBooksViewConfig::class, MyPresentsBooksViewConfig.serializer())
            }
        }

        factory { BookingViewModel(it.get(), get()) }
        factory { MyPresentsBooksViewModel(it.get(), get(), get()) }

        single<BookingModel> {
            val bookingFeature = get<BookingFeature>()
            object : BookingModel {
                override suspend fun getBookingState(itemId: WishlistItemId): BookingState? =
                    bookingFeature.getState(itemId)

                override suspend fun bookItem(itemId: WishlistItemId): Boolean =
                    bookingFeature.tryBook(itemId)

                override suspend fun cancelBooking(itemId: WishlistItemId): Boolean =
                    bookingFeature.cancelBooking(itemId)

                override suspend fun myPresentsBooks(): List<RegisteredWishlistItem> =
                    bookingFeature.myPresentsBooks()
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
