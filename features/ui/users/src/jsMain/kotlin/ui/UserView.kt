package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the public user profile detail screen. Uses Bootstrap classes. */
class UserView(
    chain: NavigationChain<ViewConfig>,
    config: UserViewConfig,
) : ComposeView<UserViewConfig, ViewConfig, UserViewModel>(config, chain) {
    override val viewModel: UserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val avatarId by viewModel.avatarIdState.collectAsState()
        val canEdit by viewModel.canEditState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(UsersListStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(UsersListStrings.profileTitle.translation())
                if (canEdit) {
                    Button({
                        classes("btn", "btn-outline-primary", "ms-auto")
                        onClick { viewModel.onEditUser() }
                    }) { Text(UsersListStrings.editButton.translation()) }
                }
            }

            if (loading) {
                P { Text(UsersListStrings.loading.translation()) }
            } else {
                avatarId?.let { id ->
                    Div({ classes("mb-3") }) {
                        Img(src = viewModel.imageUrl(id), alt = UsersListStrings.avatarLabel.translation()) {
                            classes("rounded", "border")
                            attr("width", "160")
                            attr("height", "160")
                            attr("style", "object-fit: cover;")
                        }
                    }
                }
                Div({ classes("mb-2") }) {
                    P({ classes("h5", "mb-0") }) {
                        Text(user?.username?.string ?: "#${config.userId.long}")
                    }
                }
            }
        }
    }
}
