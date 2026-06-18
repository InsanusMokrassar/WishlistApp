package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
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
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the public user profile detail screen (Calm Studio header). */
class UserView(
    chain: NavigationChain<ViewConfig>,
    config: UserViewConfig,
) : ComposeView<UserViewConfig, ViewConfig, UserViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserView)
    }

    override val title: String
        @Composable get() = UsersListStrings.profileTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val avatarId by viewModel.avatarIdState.collectAsState()
        val canEdit by viewModel.canEditState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            when {
                loading -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(UsersListStrings.loading.translation()) }
                else -> {
                    Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                        Div({
                            style {
                                property("display", "flex")
                                property("gap", "16px")
                                property("align-items", "center")
                            }
                        }) {
                            val id = avatarId
                            Span({
                                if (id == null) classes(tintClass(config.userId.long))
                                style {
                                    property("width", "60px")
                                    property("height", "60px")
                                    property("border-radius", "999px")
                                    property("display", "block")
                                    property("flex-shrink", "0")
                                    if (id != null) {
                                        property("background-image", "url(${viewModel.imageUrl(id)})")
                                        property("background-size", "cover")
                                        property("background-position", "center")
                                    }
                                }
                            })
                            H1 { Text(user?.username?.string ?: "#${config.userId.long}") }
                        }
                        if (canEdit) {
                            Div({ classes(CalmStudioStyleSheet.acts) }) {
                                Button({
                                    classes(CalmStudioStyleSheet.btn)
                                    onClick { viewModel.onEditUser() }
                                }) {
                                    CalmIcon(CalmIcons.edit)
                                    Text(UsersListStrings.editButton.translation())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
