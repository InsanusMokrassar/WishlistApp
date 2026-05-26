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
     * Called when the user confirms discarding changes or cancels without editing.
     *
     * @param node Current navigation node.
     */
    suspend fun onNavigateBack(node: NavigationNode<WishlistEditViewConfig, ViewConfig>)

    /**
     * Called after the wishlist has been successfully saved (created or updated).
     *
     * @param node Current navigation node.
     */
    suspend fun onSaved(node: NavigationNode<WishlistEditViewConfig, ViewConfig>)
}
