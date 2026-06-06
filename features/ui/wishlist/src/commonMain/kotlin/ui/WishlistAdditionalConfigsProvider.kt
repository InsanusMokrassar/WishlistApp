package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * Extension point for additional, item-scoped views drawn on the wishlist item screen.
 *
 * Each provider contributes one compact view (one button or one short text) rendered in place on
 * the item screen via [WishlistItemAdditionalConfigView]. Inline drawing is the default (when
 * [chainId] is `null`); a non-null [chainId] enables reuse of an externally hosted chain —
 * the item screen first searches the navigation tree from the root chain for an existing chain
 * with [chainId] and pushes the view into that chain, falling back to inline injection when the
 * external chain is absent (see [WishlistItemAdditionalConfigView]).
 *
 * Implementations are collected at the ViewModel factory via Koin `getAllDistinct` and injected into
 * [WishlistItemViewModel] through its constructor. The interface is `sealed`, so every implementation
 * lives in this module (`features/ui/wishlist`); a feature that wants to plug in (e.g. booking)
 * provides a thin adapter here that returns the foreign feature's own [ViewConfig].
 */
sealed interface WishlistAdditionalConfigsProvider {
    /**
     * Optional stable id of the navigation chain this provider's inline view is drawn into.
     *
     * `null` (the default): the item screen always injects the provider's view inline, in a fresh
     * anonymous chain. Non-null: the item screen first searches the navigation tree from the root
     * chain for an existing externally hosted chain with this id — when found, the view is pushed
     * into that chain (drawn wherever its host draws it); when absent, the view is injected inline
     * under this id. Each provider must use a distinct id so its view stays isolated from other
     * providers' chains and from the item screen's own chain.
     */
    val chainId: NavigationChainId?
        get() = null

    /**
     * Builds the navigation config of the inline view drawn for [item].
     *
     * @param item Item currently displayed on the wishlist item screen.
     * @return Config rendered inline (via `InjectNavigationNode`) inside this provider's chain.
     */
    fun createConfig(item: RegisteredWishlistItem): ViewConfig
}
