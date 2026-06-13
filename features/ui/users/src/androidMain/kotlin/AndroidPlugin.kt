package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditView
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserView
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListView
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/** Android startup plugin — registers the users list, profile view and profile edit node factories. */
object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<UsersListViewConfig, ViewConfig> { chain, cfg ->
                UsersListView(chain, cfg)
            }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<UserViewConfig, ViewConfig> { chain, cfg ->
                UserView(chain, cfg)
            }
        }
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<UserEditViewConfig, ViewConfig> { chain, cfg ->
                UserEditView(chain, cfg)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
