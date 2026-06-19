package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.EmptyState
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.PeopleGrid
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the Discover screen — a [PeopleGrid] of person tiles (Calm Studio).
 *
 * The person tiles stay hand-written (`.person` / `.av`) because they render a real avatar `<img>` when
 * the user has one, which the [dev.inmo.wishlist.features.common.client.ui.components.PersonCard]
 * component does not support.
 */
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
        val avatars by viewModel.avatarsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currentUserId by viewModel.currentUserIdState.collectAsState()

        ContentColumn {
            PageHead(
                title = UsersListStrings.title.translation(),
                actions = {
                    if (currentUserId != null) {
                        CalmButton(
                            text = UsersListStrings.myProfileButton.translation(),
                            onClick = { viewModel.onMyProfile() },
                        )
                    }
                },
            )

            when {
                loading -> Subline(UsersListStrings.loading.translation())
                users.isEmpty() -> EmptyState(
                    icon = CalmIcons.compass,
                    title = UsersListStrings.empty.translation(),
                )
                else -> PeopleGrid {
                    users.forEach { user ->
                        Div({
                            classes(CalmStudioStyleSheet.person)
                            onClick { viewModel.onUserSelected(user.id) }
                        }) {
                            val avatarId = avatars[user.id]
                            if (avatarId == null) {
                                Span({ classes(CalmStudioStyleSheet.av, tintClass(user.id.long)) })
                            } else {
                                Img(src = viewModel.imageUrl(avatarId), alt = "") { classes(CalmStudioStyleSheet.av) }
                            }
                            H3 { Text(user.username.string) }
                        }
                    }
                }
            }
        }
    }
}
