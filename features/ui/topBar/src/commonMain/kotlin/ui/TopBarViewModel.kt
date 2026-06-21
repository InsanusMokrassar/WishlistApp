package dev.inmo.wishlist.features.ui.topBar.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the top bar.
 *
 * Forwards the "change server URL" intent to [interactor] and tracks which screens own the top bar
 * title: it watches the whole navigation subtree under the root chain and, on each change, locates
 * the scaffold's main chain ([MainNavigationChainId]) and exposes every [TopBarTitleProvider] in
 * that chain's stack (in stack order) via [titleProviders]. The view concatenates their titles.
 *
 * The embedded auth widget owns its own state via [dev.inmo.wishlist.features.ui.auth].
 *
 * @param node Navigation node owning the top bar.
 * @param interactor Navigation delegate for top-level actions (open server URL editor).
 */
class TopBarViewModel(
    private val node: NavigationNode<TopBarViewConfig, ViewConfig>,
    private val interactor: TopBarViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val rootChain = node.chain.rootChain()

    /**
     * The scaffold's main navigation chain ([MainNavigationChainId]) as last located by the subtree
     * subscription, or `null` while it is not yet present. Held so [onCrumbSelected] can collapse the
     * chain back to a clicked breadcrumb's node without re-resolving it.
     */
    private var mainChain: NavigationChain<ViewConfig>? = null

    private val _titleProviders = MutableRedeliverStateFlow<List<TopBarTitleProvider>>(emptyList())

    /**
     * Every [TopBarTitleProvider] on the main chain's stack, in stack order (bottom → top), or an
     * empty list when none is present (the top bar then falls back to the application title). The
     * view concatenates the providers' titles into a single breadcrumb-like title.
     */
    val titleProviders = _titleProviders.asStateFlow()

    private val _searchQueryState = MutableRedeliverStateFlow("")

    /**
     * Current text of the global search field (people / lists / items). Held here so the field
     * keeps its value across recompositions; result navigation is wired in a later redesign phase.
     */
    val searchQueryState = _searchQueryState.asStateFlow()

    init {
        merge(flowOf(Unit), rootChain.changesInSubTreeFlow().map { }).subscribeLoggingDropExceptions(scope) {
            val mainChain = rootChain.findInSubTree(MainNavigationChainId)
            this.mainChain = mainChain
            _titleProviders.value = mainChain
                ?.stackFlow
                ?.value
                ?.filterIsInstance<TopBarTitleProvider>()
                ?: emptyList()
        }
    }

    /**
     * Navigates to the chain position the clicked breadcrumb segment reflects: collapses the main
     * chain by dropping every node sitting above [provider]'s node (top-down, awaiting each removal),
     * leaving [provider]'s node on top. A no-op when the main chain is unknown, [provider] is not a
     * node of it, or it is already the topmost node (clicking the current/last crumb).
     *
     * @param provider The [TopBarTitleProvider] backing the clicked crumb (it is the navigation node).
     */
    fun onCrumbSelected(provider: TopBarTitleProvider) {
        val chain = mainChain ?: return
        val targetNode = provider as? NavigationNode<*, ViewConfig> ?: return
        scope.launchLoggingDropExceptions {
            var last = chain.stackFlow.value.lastOrNull()
            while (last != null && last !== targetNode) {
                chain.drop(last)
                chain.stackFlow.first { it.lastOrNull() !== last } // await the removal before the next drop
                last = chain.stackFlow.value.lastOrNull()
            }
        }
    }

    /**
     * Updates the global search query as the user types.
     *
     * @param query New search text.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQueryState.value = query
    }

    /** Forwards "change server URL" to [TopBarViewInteractor.onChangeServerUrl]. */
    fun onChangeServerUrl() {
        scope.launchLoggingDropExceptions { interactor.onChangeServerUrl(node) }
    }
}
