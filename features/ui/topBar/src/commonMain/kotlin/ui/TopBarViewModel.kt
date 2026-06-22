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
     * The scaffold's main navigation chain, cached after the first subtree-change event.
     * Updated on every subtree-change alongside [_titleProviders].
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
            val resolved = rootChain.findInSubTree(MainNavigationChainId)
            mainChain = resolved
            _titleProviders.value = resolved
                ?.stackFlow
                ?.value
                ?.filterIsInstance<TopBarTitleProvider>()
                ?: emptyList()
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

    /**
     * Handles a user tap on a non-current breadcrumb segment.
     *
     * Collapses the main navigation chain so that [provider] becomes the top-most node. The
     * nodes to drop are collected into an explicit list up front ([toDrop]), then dropped
     * top-first. Each drop is awaited before the next is issued because
     * [dev.inmo.navigation.core.NavigationChain.drop] snapshots `newStack` at call-time and
     * applies it on a FIFO channel — two un-awaited drops would each snapshot the same pre-drop
     * stack and the second would re-add the node the first removed. [NavigationChain] exposes no
     * race-free batch-drop API, so the per-node await is forced, not lazy.
     *
     * No-ops when [provider] is not found in the chain, is already the top node, the chain is
     * not yet resolved, or [provider] is not a [dev.inmo.navigation.core.NavigationNode].
     *
     * @param provider The [TopBarTitleProvider] node to navigate back to.
     */
    fun onCrumbSelected(provider: TopBarTitleProvider) {
        val chain = mainChain ?: return
        val targetNode = provider as? NavigationNode<*, ViewConfig> ?: return
        // Build the explicit drop-list ONCE: every node strictly above targetNode, in stack order
        // (bottom→top). takeLastWhile stops at the first node identity-equal to targetNode, so the
        // result excludes targetNode and everything below it.
        val toDrop = chain.stackFlow.value.takeLastWhile { it !== targetNode }
        // No-op when targetNode is absent (toDrop == whole stack), or is already the top (toDrop empty).
        if (toDrop.isEmpty() || toDrop.size == chain.stackFlow.value.size) return
        scope.launchLoggingDropExceptions {
            // Drop the pre-computed nodes top-first. Each drop is awaited before the next is issued:
            // NavigationChain.drop snapshots `newStack` at call-time and applies it on a FIFO channel,
            // so two un-awaited drops would each snapshot the same pre-drop stack and the second would
            // re-add the node the first removed. NavigationChain offers no race-free batch-drop API,
            // so the per-node await is forced, not lazy. The drop-list itself is now explicit and
            // pre-built (reviewer's "list of nodes to drop") rather than recomputed each loop.
            for (node in toDrop.asReversed()) {
                chain.drop(node)
                chain.stackFlow.first { node !in it }
            }
        }
    }

    /** Forwards "change server URL" to [TopBarViewInteractor.onChangeServerUrl]. */
    fun onChangeServerUrl() {
        scope.launchLoggingDropExceptions { interactor.onChangeServerUrl(node) }
    }
}
