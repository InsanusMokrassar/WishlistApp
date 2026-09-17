package dev.inmo.wishlist.features.ui.sidebar

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.sidebar.ui.DefaultSidebarModel
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarModel
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewConfig
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the sidebar UI feature.
 *
 * Registers the polymorphic serializer for [SidebarViewConfig], the [SidebarViewModel] factory, and
 * a [SidebarModel] singleton that composes the already-registered [WishlistsModel], [BookingModel]
 * and [UsersModel] singletons (all resolved lazily so this plugin needs no cross-feature `setupDI`
 * delegation).
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
            DefaultSidebarModel(
                wishlistsModel = get(),
                bookingModel = get(),
                usersModel = get(),
            )
        }

        factory { SidebarViewModel(node = it.get(), model = get(), interactor = get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
