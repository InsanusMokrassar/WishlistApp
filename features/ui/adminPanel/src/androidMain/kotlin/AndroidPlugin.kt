package dev.inmo.wishlist.features.ui.adminPanel

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListView
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListViewConfig

/**
 * Android platform startup plugin for the admin panel UI feature.
 *
 * Delegates to [Plugin] for common DI and registers [NavigationNodeFactory] entries
 * for all eight Android Compose-Material3 views.
 */
object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminPanelViewConfig, ViewConfig> { chain, cfg -> AdminPanelView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminUsersListViewConfig, ViewConfig> { chain, cfg -> AdminUsersListView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminUserViewConfig, ViewConfig> { chain, cfg -> AdminUserView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminUserEditViewConfig, ViewConfig> { chain, cfg -> AdminUserEditView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminWishlistsListViewConfig, ViewConfig> { chain, cfg -> AdminWishlistsListView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminWishlistViewConfig, ViewConfig> { chain, cfg -> AdminWishlistView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminWishlistEditViewConfig, ViewConfig> { chain, cfg -> AdminWishlistEditView(chain, cfg) }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<AdminWishlistItemEditViewConfig, ViewConfig> { chain, cfg -> AdminWishlistItemEditView(chain, cfg) }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}