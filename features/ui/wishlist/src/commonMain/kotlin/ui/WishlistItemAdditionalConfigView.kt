package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * Draws one [WishlistAdditionalConfigsProvider] contribution for [item].
 *
 * Behavior contract (PR #31 T5, operator decision):
 * 1. [WishlistAdditionalConfigsProvider.chainId] == `null` → the provider's view is injected
 *    inline right here, in a fresh anonymous chain.
 * 2. chainId != `null` → a [LaunchedEffect] searches the navigation tree from the root chain for an
 *    existing chain with that id. Found external chain → the provider's config is pushed into that
 *    chain (the view renders wherever the host draws the chain); a host that never draws its chain
 *    keeps the view invisible — host contract, not guarded here. Not found → inline injection
 *    under [WishlistAdditionalConfigsProvider.chainId].
 *
 * Safeguards:
 * - Stale-self exclusion: a previous inline injection of this same screen leaves its chain in the
 *   navigation tree after pause/resume (the composition is disposed, the chain is not). A found
 *   chain lying inside [viewNode]'s own subtree is therefore dropped ([NavigationChain.dropItself])
 *   and the search retried, so the view never renders into an undrawn leftover chain. The retry loop
 *   terminates because each iteration removes one chain from the finite tree, or exits when
 *   [NavigationChain.dropItself] returns `false` (removal refused, fall back to inline).
 * - Duplicate-push guard: the config is pushed only when no node with a structurally equal config
 *   is already in the target stack; recompositions reuse the existing node.
 * - Cleanup: a node pushed into an external chain is dropped in the [DisposableEffect] dispose so
 *   leaving the item screen removes the pushed view.
 * - Nothing is rendered while the search is in flight (single frame) to avoid a transient inline
 *   chain that would immediately become stale.
 *
 * @param provider Provider whose compact view is drawn.
 * @param item Item currently displayed; forwarded to [WishlistAdditionalConfigsProvider.createConfig].
 * @param viewNode Navigation node of the item screen; anchor for root-chain search and for the
 *   own-subtree staleness test.
 */
@Composable
fun WishlistItemAdditionalConfigView(
    provider: WishlistAdditionalConfigsProvider,
    item: RegisteredWishlistItem,
    viewNode: NavigationNode<*, ViewConfig>,
) {
    val config = remember(provider, item) { provider.createConfig(item) }
    val chainId = provider.chainId
    if (chainId == null) {
        InjectNavigationChain<ViewConfig> {
            InjectNavigationNode(config)
        }
    } else {
        var searched by remember(chainId) { mutableStateOf(false) }
        var externalChain by remember(chainId) { mutableStateOf<NavigationChain<ViewConfig>?>(null) }
        LaunchedEffect(chainId) {
            var candidate = viewNode.chain.rootChain().findInSubTree(chainId)
            while (candidate != null && candidate.isInSubTreeOf(viewNode)) {
                candidate = if (candidate.dropItself()) {
                    viewNode.chain.rootChain().findInSubTree(chainId)
                } else {
                    null // removal refused — treat as not found, fall back to inline
                }
            }
            externalChain = candidate
            searched = true
        }
        val target = externalChain
        when {
            !searched -> Unit // search in flight, render nothing this frame
            target != null -> DisposableEffect(target, config) {
                val pushed = if (target.stackFlow.value.none { it.config == config }) {
                    target.push(config)
                } else {
                    null
                }
                onDispose { pushed?.let { target.drop(it) } }
            }
            else -> InjectNavigationChain<ViewConfig>(id = chainId) {
                InjectNavigationNode(config)
            }
        }
    }
}

/**
 * Whether this chain lies inside the navigation subtree rooted at [node]: walks parent chains
 * upward and reports `true` when any of them is parented by [node] itself.
 *
 * Used to detect stale self-injected chains left in the navigation tree after pause/resume —
 * a chain that is in [node]'s own subtree is a leftover, not an external host chain.
 *
 * @param node Subtree root candidate.
 * @return `true` when this chain is a (transitive) subchain of [node].
 */
private fun NavigationChain<ViewConfig>.isInSubTreeOf(node: NavigationNode<*, ViewConfig>): Boolean =
    generateSequence(this) { it.parentNode?.chain }.any { it.parentNode === node }
