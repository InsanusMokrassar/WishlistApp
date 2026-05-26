package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditView
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditView
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemView
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistView
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListView
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS platform startup plugin for the wishlist UI feature.
 *
 * Delegates to [Plugin] for common DI and registers [NavigationNodeFactory] entries
 * for all four JS Compose-HTML views.
 */
object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<WishlistsListViewConfig, ViewConfig> { chain, cfg -> WishlistsListView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<WishlistViewConfig, ViewConfig> { chain, cfg -> WishlistView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<WishlistEditViewConfig, ViewConfig> { chain, cfg -> WishlistEditView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<WishlistItemEditViewConfig, ViewConfig> { chain, cfg -> WishlistItemEditView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<WishlistItemViewConfig, ViewConfig> { chain, cfg -> WishlistItemView(chain, cfg) }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
