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
     * Called when the user confirms discarding changes or cancels without editing.
     *
     * @param node Current navigation node.
     */
    suspend fun onNavigateBack(node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>)

    /**
     * Called after the wishlist item has been successfully saved (created or updated).
     *
     * @param node Current navigation node.
     */
    suspend fun onSaved(node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>)
}
