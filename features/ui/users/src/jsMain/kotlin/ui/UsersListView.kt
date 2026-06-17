package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the Discover screen — a people grid (Calm Studio `.people` / `.person`). */
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

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div { H1 { Text(UsersListStrings.title.translation()) } }
                Div({ classes("acts") }) {
                    if (currentUserId != null) {
                        Button({
                            classes("btn")
                            onClick { viewModel.onMyProfile() }
                        }) { Text(UsersListStrings.myProfileButton.translation()) }
                    }
                }
            }

            when {
                loading -> P({ classes("subline") }) { Text(UsersListStrings.loading.translation()) }
                users.isEmpty() -> Div({ classes("empty") }) {
                    Div({ classes("ic") }) { CalmIcon(CalmIcons.compass) }
                    H3 { Text(UsersListStrings.empty.translation()) }
                }
                else -> Div({ classes("people") }) {
                    users.forEach { user ->
                        Div({
                            classes("person")
                            onClick { viewModel.onUserSelected(user.id) }
                        }) {
                            val avatarId = avatars[user.id]
                            Span({
                                if (avatarId == null) {
                                    classes("av", tintClass(user.id.long))
                                } else {
                                    classes("av")
                                    style {
                                        property("background-image", "url(${viewModel.imageUrl(avatarId)})")
                                        property("background-size", "cover")
                                        property("background-position", "center")
                                    }
                                }
                            })
                            H3 { Text(user.username.string) }
                        }
                    }
                }
            }
        }
    }
}
