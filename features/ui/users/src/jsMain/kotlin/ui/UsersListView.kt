package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the users list screen. Uses Bootstrap classes. */
class UsersListView(
    chain: NavigationChain<ViewConfig>,
    config: UsersListViewConfig,
) : ComposeView<UsersListViewConfig, ViewConfig, UsersListViewModel>(config, chain) {
    override val viewModel: UsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UsersListView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            H1({ classes("h3", "mb-3") }) { Text(UsersListStrings.title.translation()) }
            if (loading) {
                P { Text(UsersListStrings.loading.translation()) }
            } else if (users.isEmpty()) {
                P({ classes("text-muted") }) { Text(UsersListStrings.empty.translation()) }
            } else {
                Ul({ classes("list-group") }) {
                    users.forEach { user ->
                        Li({
                            classes("list-group-item", "list-group-item-action")
                            style { property("cursor", "pointer") }
                            onClick { viewModel.onUserSelected(user.id) }
                        }) {
                            Span { Text(user.username.string) }
                        }
                    }
                }
            }
        }
    }
}
