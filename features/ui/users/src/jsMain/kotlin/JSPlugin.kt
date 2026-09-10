package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditView
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeView
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserView
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListView
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/** JS startup plugin registering list, profile, editor, and pending/completed password factories. */
object JSPlugin : StartPlugin {
    /** Delegates shared users bindings and registers all four Compose HTML node factories. */
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
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<PasswordChangeViewConfig, ViewConfig> { chain, cfg ->
                PasswordChangeView(chain, cfg)
            }
        }
    }

    /** Starts shared users feature initialization after platform registrations are available. */
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
