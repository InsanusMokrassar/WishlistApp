package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the users list screen. Uses Bootstrap classes. */
class UsersListView(
    chain: NavigationChain<ViewConfig>,
    config: UsersListViewConfig,
) : ComposeView<UsersListViewConfig, ViewConfig, UsersListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UsersListView)
    }

    override val title: String
        @Composable get() = UsersListStrings.title.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currentUserId by viewModel.currentUserIdState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-3") }) {
                if (currentUserId != null) {
                    Button({
                        classes("btn", "btn-outline-primary")
                        onClick { viewModel.onMyProfile() }
                    }) { Text(UsersListStrings.myProfileButton.translation()) }
                }
            }
            if (loading) {
                P { Text(UsersListStrings.loading.translation()) }
            } else if (users.isEmpty()) {
                P({ classes("text-muted") }) { Text(UsersListStrings.empty.translation()) }
            } else {
                Ul({ classes("list-group") }) {
                    users.forEach { user ->
                        ListRow(
                            text = user.username.string,
                            onSelect = { viewModel.onUserSelected(user.id) }
                        )
                    }
                }
            }
        }
    }
}
