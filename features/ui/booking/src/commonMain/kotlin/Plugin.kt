package dev.inmo.wishlist.features.ui.booking

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.booking.client.BookingFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewModel
import dev.inmo.wishlist.features.ui.booking.ui.DefaultBookingModel
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewModel
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
            DefaultBookingModel(bookingFeature = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
