package dev.inmo.wishlist.features.ui.scaffold.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
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
 * Each slot is rendered through [ScaffoldSlot] instead of a fresh `InjectNavigationChain`, so a
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
     * If a sub-chain with the given [id] is **already attached** to this node — i.e. it was created
     * while restoring the hierarchy from the URL — that chain (with its restored stack) is drawn via
     * the protected [SubchainsHost], so shared/reloaded deep links keep their content. Otherwise the
     * slot is rendered with a plain `InjectNavigationChain(id) { InjectNavigationNode(slotConfig) }`,
     * the framework's standard path that seeds the chain with [slotConfig] and drives its lifecycle.
     *
     * The restored-vs-fresh decision is re-evaluated whenever this node's [subchainsFlow] changes:
     * if a sub-chain with [id] appears (e.g. one attached while restoring the hierarchy), the slot
     * switches to drawing that restored chain; otherwise it keeps the freshly injected one.
     *
     * @param id Stable identifier distinguishing this slot's chain from the other slots.
     * @param slotConfig Config used as the slot's first node when no restored chain exists.
     */
    @Composable
    private fun ScaffoldSlot(id: NavigationChainId, slotConfig: ViewConfig) {
        val subchainsFlow = subchainsFlow.collectAsState()
        val restoredChain = remember(id, subchainsFlow.value) { subchainsFlow.value.firstOrNull { it.id == id } }
        if (restoredChain != null) {
            SubchainsHost { it === restoredChain }
        } else {
            InjectNavigationChain<ViewConfig>(id = id) { InjectNavigationNode(slotConfig) }
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val cfg = viewModel.config

        Div({ classes(ScaffoldViewStylesheet.scaffoldContainer) }) {
            cfg.topConfig?.let { topConfig ->
                Div {
                    ScaffoldSlot(TopNavigationChainId, topConfig)
                }
            }
            Div({ classes(ScaffoldViewStylesheet.scaffoldBody) }) {
                cfg.leftConfig?.let { leftConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldLeft) }) {
                        ScaffoldSlot(LeftNavigationChainId, leftConfig)
                    }
                }
                cfg.mainConfig?.let { mainConfig ->
                    Div({ classes(ScaffoldViewStylesheet.scaffoldMain) }) {
                        ScaffoldSlot(MainNavigationChainId, mainConfig)
                    }
                }
            }
        }
    }
}
