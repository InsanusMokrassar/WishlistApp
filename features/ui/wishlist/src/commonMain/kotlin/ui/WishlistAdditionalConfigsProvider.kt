package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * Extension point for additional, item-scoped views drawn on the wishlist item screen.
 *
 * Each provider contributes one compact view (one button or one short text) rendered in place on
 * the item screen via [WishlistItemAdditionalConfigView], which injects the view inline in a fresh
 * anonymous chain.
 *
 * Implementations are collected at the ViewModel factory via Koin `getAllDistinct` and injected into
 * [WishlistItemViewModel] through its constructor. The interface is `sealed`, so every implementation
 * lives in this module (`features/ui/wishlist`); a feature that wants to plug in (e.g. booking)
 * provides a thin adapter here that returns the foreign feature's own [ViewConfig].
 */
sealed interface WishlistAdditionalConfigsProvider {
    /**
     * Builds the navigation config of the inline view drawn for [item].
     *
     * @param item Item currently displayed on the wishlist item screen.
     * @return Config rendered inline (via `InjectNavigationNode`) inside this provider's chain.
     */
    fun createConfig(item: RegisteredWishlistItem): ViewConfig
}
