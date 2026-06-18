package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin panel dashboard screen (Calm Studio). */
class AdminPanelView(
    chain: NavigationChain<ViewConfig>,
    config: AdminPanelViewConfig,
) : ComposeView<AdminPanelViewConfig, ViewConfig, AdminPanelViewModel>(config, chain) {
    override val viewModel: AdminPanelViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminPanelView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 { Text(AdminPanelStrings.title.translation()) }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                        onClick { viewModel.onOpenUsers() }
                    }) {
                        Text(AdminPanelStrings.usersSection.translation())
                    }
                    Button({
                        classes(CalmStudioStyleSheet.btn)
                        onClick { viewModel.onOpenWishlists() }
                    }) {
                        Text(AdminPanelStrings.wishlistsSection.translation())
                    }
                }
            }
        }
    }
}
