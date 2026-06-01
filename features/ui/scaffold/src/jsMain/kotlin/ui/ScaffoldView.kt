package dev.inmo.wishlist.features.ui.scaffold.ui

import androidx.compose.runtime.Composable
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.Div
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the scaffold layout.
 *
 * Renders up to three navigation sub-chains laid out as:
 * - top slot: full-width row at the top
 * - left slot: fixed left column below the top slot
 * - main slot: flexible area filling remaining width and height
 *
 * A slot is only rendered when the corresponding config in [ScaffoldViewConfig] is non-null.
 */
class ScaffoldView(
    chain: NavigationChain<ViewConfig>,
    config: ScaffoldViewConfig,
) : ComposeView<ScaffoldViewConfig, ViewConfig, ScaffoldViewModel>(config, chain) {

    override val viewModel: ScaffoldViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@ScaffoldView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val cfg = viewModel.config

        Div({ classes(ScaffoldViewStylesheet.scaffoldContainer) }) {
            cfg.topConfig?.let { topConfig ->
                Div {
                    InjectNavigationChain<ViewConfig> { InjectNavigationNode(topConfig) }
                }
            }
            Div({ classes(ScaffoldViewStylesheet.scaffoldBody) }) {
                cfg.leftConfig?.let { leftConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldLeft) }) {
                        InjectNavigationChain<ViewConfig> { InjectNavigationNode(leftConfig) }
                    }
                }
                cfg.mainConfig?.let { mainConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldMain) }) {
                        InjectNavigationChain<ViewConfig>(id = MainNavigationChainId) { InjectNavigationNode(mainConfig) }
                    }
                }
            }
        }
    }
}
