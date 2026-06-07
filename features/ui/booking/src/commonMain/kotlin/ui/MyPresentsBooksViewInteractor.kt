package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [MyPresentsBooksViewModel].
 *
 * Exposes the navigation side-effect the my-presents view delegates to the surrounding
 * application. The implementation is registered in `client/ClientPlugin`.
 */
interface MyPresentsBooksViewInteractor {
    /**
     * Called when the user navigates back from the my-presents view.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<MyPresentsBooksViewConfig, ViewConfig>)
}
