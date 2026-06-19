package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Per-view stylesheet for [UserView]: the profile header row and the round avatar tile (rendered as a
 * tinted [Span] placeholder or a cover `<img>`). Self-registers into the [StyleSheetsAggregator].
 */
object UserViewStylesheet : StyleSheet() {
    /** Profile header: avatar + name laid out as a centered flex row. */
    val profileHead by style {
        property("display", "flex"); property("gap", "16px"); property("align-items", "center")
    }

    /** Round 60px avatar tile (covers when applied to an `<img>`). */
    val avatar by style {
        property("width", "60px"); property("height", "60px"); property("border-radius", "999px")
        property("display", "block"); property("flex-shrink", "0"); property("object-fit", "cover")
    }

    init { StyleSheetsAggregator.addStyleSheet(this) }
}

/**
 * JS Compose-HTML view for the public user profile detail screen (Calm Studio header).
 *
 * The header keeps a hand-written `.pagehead` because its title is an avatar + name cluster rather than
 * the plain string title the [PageHead] component takes.
 */
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

        ContentColumn {
            when {
                loading -> Subline(UsersListStrings.loading.translation())
                else -> {
                    Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                        Div({ classes(UserViewStylesheet.profileHead) }) {
                            val id = avatarId
                            if (id == null) {
                                Span({ classes(UserViewStylesheet.avatar, tintClass(config.userId.long)) })
                            } else {
                                Img(src = viewModel.imageUrl(id), alt = "") { classes(UserViewStylesheet.avatar) }
                            }
                            H1 { Text(user?.username?.string ?: "#${config.userId.long}") }
                        }
                        if (canEdit) {
                            Div({ classes(CalmStudioStyleSheet.acts) }) {
                                CalmButton(
                                    text = UsersListStrings.editButton.translation(),
                                    onClick = { viewModel.onEditUser() },
                                    leadingIcon = CalmIcons.edit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
