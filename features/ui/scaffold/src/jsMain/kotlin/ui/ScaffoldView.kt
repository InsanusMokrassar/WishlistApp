package dev.inmo.wishlist.features.ui.scaffold.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.LeftNavigationChainId
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.TopNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
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
 *
 * Each slot is rendered through [scaffoldSlot] instead of a fresh `InjectNavigationChain`, so a
 * navigation hierarchy restored from the URL (via
 * [dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo]) is reattached to its slot by
 * matching the slot's stable [NavigationChainId]. Without this, the scaffold would always inject
 * empty chains and discard the restored deep-link stack.
 */
class ScaffoldView(
    chain: NavigationChain<ViewConfig>,
    config: ScaffoldViewConfig,
) : ComposeView<ScaffoldViewConfig, ViewConfig, ScaffoldViewModel>(config, chain) {

    override val viewModel: ScaffoldViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@ScaffoldView)
    }

    /**
     * Renders one scaffold slot's navigation sub-chain.
     *
     * Reuses an already-attached sub-chain with the given [id] (e.g. one produced while restoring
     * the hierarchy from the URL) when present; otherwise creates the sub-chain and seeds it with
     * [slotConfig]. The chain is drawn via [SubchainsHost] filtered by [id].
     *
     * @param id Stable identifier distinguishing this slot's chain from the other slots.
     * @param slotConfig Config pushed as the slot's first node when no restored chain exists.
     */
    @Composable
    private fun scaffoldSlot(id: NavigationChainId, slotConfig: ViewConfig) {
        remember(id) {
            if (subchainsFlow.value.none { it.id == id }) {
                createEmptySubChain(id).also { it.push(slotConfig) }
            }
        }
        SubchainsHost { it.id == id }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val cfg = viewModel.config

        Div({ classes(ScaffoldViewStylesheet.scaffoldContainer) }) {
            cfg.topConfig?.let { topConfig ->
                Div {
                    scaffoldSlot(TopNavigationChainId, topConfig)
                }
            }
            Div({ classes(ScaffoldViewStylesheet.scaffoldBody) }) {
                cfg.leftConfig?.let { leftConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldLeft) }) {
                        scaffoldSlot(LeftNavigationChainId, leftConfig)
                    }
                }
                cfg.mainConfig?.let { mainConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldMain) }) {
                        scaffoldSlot(MainNavigationChainId, mainConfig)
                    }
                }
            }
        }
    }
}
