package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * Extension point for additional, item-scoped views drawn INLINE on the wishlist item screen.
 *
 * Each provider contributes one compact view (one button or one short text) that is rendered in
 * place on the item screen — not behind a button that pushes a new screen. The item view draws each
 * provider by injecting a dedicated navigation chain and node:
 * `InjectNavigationChain(id = provider.chainId) { InjectNavigationNode(provider.createConfig(item)) }`.
 *
 * Implementations are collected at the ViewModel factory via Koin `getAllDistinct` and injected into
 * [WishlistItemViewModel] through its constructor. The interface is `sealed`, so every implementation
 * lives in this module (`features/ui/wishlist`); a feature that wants to plug in (e.g. booking)
 * provides a thin adapter here that returns the foreign feature's own [ViewConfig].
 */
sealed interface WishlistAdditionalConfigsProvider {
    /**
     * Stable, unique id of the navigation chain this provider's inline view is drawn into.
     *
     * Each provider must use a distinct id so its inline view lives in its own chain/node, isolated
     * from the other providers' chains and from the item screen's own chain.
     */
    val chainId: NavigationChainId

    /**
     * Builds the navigation config of the inline view drawn for [item].
     *
     * @param item Item currently displayed on the wishlist item screen.
     * @return Config rendered inline (via `InjectNavigationNode`) inside this provider's chain.
     */
    fun createConfig(item: RegisteredWishlistItem): ViewConfig
}
