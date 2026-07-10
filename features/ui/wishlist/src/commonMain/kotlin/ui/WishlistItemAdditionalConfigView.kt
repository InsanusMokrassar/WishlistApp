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
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem

/**
 * Draws one [WishlistAdditionalConfigsProvider] contribution for [item].
 *
 * The provider's view is always injected inline right here, in a fresh anonymous chain.
 * The earlier external-chain routing (provider-declared chain ids searched from the root chain,
 * push-into-host-chain with inline fallback) was removed in favor of this always-inline behavior.
 *
 * @param provider Provider whose compact view is drawn.
 * @param item Item currently displayed; forwarded to [WishlistAdditionalConfigsProvider.createConfig].
 * @param viewNode Navigation node of the item screen. Currently unused — retained in the signature
 *   from the removed external-chain routing.
 */
@Composable
fun WishlistItemAdditionalConfigView(
    provider: WishlistAdditionalConfigsProvider,
    item: WishlistsFeatureItem,
    viewNode: NavigationNode<*, ViewConfig>,
) {
    val config = remember(provider, item) { provider.createConfig(item) }
    InjectNavigationChain<ViewConfig> {
        InjectNavigationNode(config)
    }
}

/**
 * Whether this chain lies inside the navigation subtree rooted at [node]: walks parent chains
 * upward and reports `true` when any of them is parented by [node] itself.
 *
 * Retained from the removed external-chain routing (where a found chain inside [node]'s own
 * subtree was a stale leftover, not an external host chain); currently unused.
 *
 * @param node Subtree root candidate.
 * @return `true` when this chain is a (transitive) subchain of [node].
 */
private fun NavigationChain<ViewConfig>.isInSubTreeOf(node: NavigationNode<*, ViewConfig>): Boolean =
    generateSequence(this) { it.parentNode?.chain }.any { it.parentNode === node }
