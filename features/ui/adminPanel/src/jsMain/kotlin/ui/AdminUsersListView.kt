package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin users list screen. Uses Bootstrap classes. */
class AdminUsersListView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUsersListViewConfig,
) : ComposeView<AdminUsersListViewConfig, ViewConfig, AdminUsersListViewModel>(config, chain) {
    override val viewModel: AdminUsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUsersListView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "justify-content-between", "align-items-center", "mb-3") }) {
                H1({ classes("h3", "mb-0") }) {
                    Text(AdminPanelStrings.usersListTitle.translation())
                }
                Button({
                    classes("btn", "btn-primary")
                    onClick { viewModel.onCreateUser() }
                }) {
                    Text(AdminPanelStrings.addUserButton.translation())
                }
            }
            if (loading) {
                P { Text(AdminPanelStrings.loading.translation()) }
            } else if (users.isEmpty()) {
                P({ classes("text-muted") }) { Text(AdminPanelStrings.emptyUsers.translation()) }
            } else {
                Ul({ classes("list-group") }) {
                    users.forEach { user ->
                        Li({
                            classes("list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center")
                            style { property("cursor", "pointer") }
                            onClick { viewModel.onUserSelected(user.id) }
                        }) {
                            Span { Text(user.username.string) }
                            Span({ classes("badge", "bg-secondary") }) { Text("#${user.id.long}") }
                        }
                    }
                }
            }
        }
    }
}
