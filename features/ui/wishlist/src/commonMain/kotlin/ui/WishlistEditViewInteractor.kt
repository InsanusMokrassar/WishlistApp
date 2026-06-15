package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [WishlistEditViewModel].
 *
 * Exposes navigation side-effects that [WishlistEditViewModel] delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistEditViewInteractor {
    /**
     * Called when the user leaves the create form (CREATE mode back, or after save/delete): pops
     * the current node off the chain, returning to wherever the user came from.
     *
     * @param node Current navigation node.
     */
    suspend fun onNavigateBack(node: NavigationNode<WishlistEditViewConfig, ViewConfig>)

    /**
     * Called when the user navigates back from EDIT mode: replaces the current node with the edited
     * wishlist's detail screen ([WishlistViewConfig]) so Back leads to the logical parent.
     *
     * @param node Current navigation node; its [WishlistEditViewConfig.wishlistId] must be non-null.
     */
    suspend fun onNavigateBackToParent(node: NavigationNode<WishlistEditViewConfig, ViewConfig>)

    /**
     * Called after the wishlist has been successfully saved (created or updated).
     *
     * @param node Current navigation node.
     */
    suspend fun onSaved(node: NavigationNode<WishlistEditViewConfig, ViewConfig>)
}
