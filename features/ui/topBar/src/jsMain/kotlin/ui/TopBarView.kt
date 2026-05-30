package dev.inmo.wishlist.features.ui.topBar.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.topBar.TopBarStrings
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class TopBarView(
    chain: NavigationChain<ViewConfig>,
    config: TopBarViewConfig,
) : ComposeView<TopBarViewConfig, ViewConfig, TopBarViewModel>(config, chain) {
    override val viewModel: TopBarViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@TopBarView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        Nav({ classes("navbar", "navbar-expand", "navbar-dark", "bg-primary") }) {
            Div({ classes("container-fluid") }) {
                A(href = "#", { classes("navbar-brand") }) {
                    Text(TopBarStrings.appTitle.translation())
                }
                Div({ classes("d-flex") }) {
                    InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }
                }
            }
        }
    }
}
