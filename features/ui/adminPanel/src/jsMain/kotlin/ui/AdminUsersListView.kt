package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.RowsList
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
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

        ContentColumn {
            PageHead(
                title = AdminPanelStrings.usersListTitle.translation(),
                actions = {
                    CalmButton(
                        text = AdminPanelStrings.addUserButton.translation(),
                        onClick = { viewModel.onCreateUser() },
                        variant = CalmButtonVariant.Primary,
                    )
                },
            )
            when {
                loading -> Subline(AdminPanelStrings.loading.translation())
                users.isEmpty() -> Subline(AdminPanelStrings.emptyUsers.translation())
                else -> RowsList {
                    users.forEach { user ->
                        ListRow(onSelect = { viewModel.onUserSelected(user.id) }) {
                            Span { Text(user.username.string) }
                            Span({ classes(CalmStudioStyleSheet.pill) }) { Text("#${user.id.long}") }
                        }
                    }
                }
            }
        }
    }
}
