package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [WishlistItemEditViewModel].
 *
 * Exposes navigation side-effects that [WishlistItemEditViewModel] delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistItemEditViewInteractor {
    /**
     * Called when the user leaves the form after a successful save or delete: pops the current node
     * off the chain, returning to wherever the user came from.
     *
     * @param node Current navigation node.
     */
    suspend fun onNavigateBack(node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>)

    /**
     * Called when the user navigates back (or confirms discarding changes): replaces the current node
     * with the logical parent so Back leads there. In EDIT mode
     * ([WishlistItemEditViewConfig.wishlistItemId] non-null) the parent is the item's read view
     * ([WishlistItemViewConfig]); in CREATE mode it is the containing wishlist
     * ([WishlistViewConfig]).
     *
     * @param node Current navigation node.
     */
    suspend fun onNavigateBackToParent(node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>)

    /**
     * Called after the wishlist item has been successfully saved (created or updated).
     *
     * @param node Current navigation node.
     */
    suspend fun onSaved(node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>)
}
