package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin users list screen (Calm Studio). */
class AdminUsersListView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUsersListViewConfig,
) : ComposeView<AdminUsersListViewConfig, ViewConfig, AdminUsersListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminUsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUsersListView)
    }

    override val title: String
        @Composable get() = AdminPanelStrings.usersListTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(AdminPanelStrings.usersListTitle.translation()) }
                }
                Div({ classes("acts") }) {
                    Button({
                        classes("btn", "primary")
                        onClick { viewModel.onCreateUser() }
                    }) {
                        Text(AdminPanelStrings.addUserButton.translation())
                    }
                }
            }
            when {
                loading -> P({ classes("subline") }) { Text(AdminPanelStrings.loading.translation()) }
                users.isEmpty() -> P({ classes("subline") }) { Text(AdminPanelStrings.emptyUsers.translation()) }
                else -> Div({ classes("rows") }) {
                    users.forEach { user ->
                        ListRow(onSelect = { viewModel.onUserSelected(user.id) }) {
                            Span { Text(user.username.string) }
                            Span({ classes("pill") }) { Text("#${user.id.long}") }
                        }
                    }
                }
            }
        }
    }
}
