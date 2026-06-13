package dev.inmo.wishlist.features.ui.scaffold.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.LeftNavigationChainId
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.TopNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JVM Compose-Desktop view for the scaffold layout.
 *
 * Renders up to three navigation sub-chains laid out as:
 * - top slot: full-width row at the top of the screen
 * - left slot: fixed left column below the top slot
 * - main slot: flexible [Box] filling remaining width and height
 *
 * A slot is only rendered when the corresponding config in [ScaffoldViewConfig] is non-null.
 *
 * Each slot's `InjectNavigationChain` is given a stable [dev.inmo.navigation.core.NavigationChainId]
 * (`TopNavigationChainId` / `LeftNavigationChainId` / `MainNavigationChainId`) so the chains are
 * addressable by slot — e.g. the top bar locates the main chain by id, and a restored hierarchy can
 * be matched to the correct slot. This platform always injects fresh chains (it uses the in-memory
 * navigation repo, so there is no restore path to reattach).
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

        Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            cfg.topConfig?.let { topConfig ->
                InjectNavigationChain<ViewConfig>(id = TopNavigationChainId) { InjectNavigationNode(topConfig) }
            }
            Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth().weight(1f)) {
                cfg.leftConfig?.let { leftConfig ->
                    InjectNavigationChain<ViewConfig>(id = LeftNavigationChainId) { InjectNavigationNode(leftConfig) }
                }
                cfg.mainConfig?.let { mainConfig ->
                    Box(modifier = androidx.compose.ui.Modifier.weight(1f).fillMaxHeight()) {
                        InjectNavigationChain<ViewConfig>(id = MainNavigationChainId) { InjectNavigationNode(mainConfig) }
                    }
                }
            }
        }
    }
}
