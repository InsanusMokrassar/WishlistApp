package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [WishlistItemCopyViewModel].
 *
 * Exposes navigation side-effects that the copy-target picker delegates to the surrounding
 * application layer. The implementation is registered in `client/ClientPlugin`.
 */
interface WishlistItemCopyViewInteractor {
    /**
     * Called when the user dismisses the picker without copying.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>)

    /**
     * Called after the item was successfully copied into the selected target wishlist.
     *
     * @param node Current navigation node.
     */
    suspend fun onCopied(node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>)

    /**
     * Called when the user chooses to create a new wishlist to copy into. Pushes the wishlist
     * create form onto the navigation chain.
     *
     * @param node Current navigation node.
     */
    suspend fun onCreateWishlist(node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>)
}
