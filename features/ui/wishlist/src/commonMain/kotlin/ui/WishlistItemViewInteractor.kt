package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [WishlistItemViewModel].
 *
 * Exposes navigation side-effects that [WishlistItemViewModel] delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistItemViewInteractor {
    /**
     * Called when the user navigates back from the item view screen.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<WishlistItemViewConfig, ViewConfig>)

    /**
     * Called when the owner taps the Edit button to open the item edit screen.
     *
     * @param node Current navigation node.
     */
    suspend fun onEditItem(node: NavigationNode<WishlistItemViewConfig, ViewConfig>)

    /**
     * Called when the user taps an additional-config provider's button on the item screen.
     *
     * @param node Current navigation node.
     * @param config Config produced by the provider; pushed onto the navigation chain.
     */
    suspend fun onAdditionalConfig(node: NavigationNode<WishlistItemViewConfig, ViewConfig>, config: ViewConfig)
}
