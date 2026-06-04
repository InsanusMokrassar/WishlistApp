package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Interactor for [BookingViewModel].
 *
 * Exposes the navigation side-effect the booking view delegates to the surrounding application.
 * The implementation is registered in `client/ClientPlugin`.
 */
interface BookingViewInteractor {
    /**
     * Called when the user navigates back from the booking view.
     *
     * @param node Current navigation node.
     */
    suspend fun onBack(node: NavigationNode<BookingViewConfig, ViewConfig>)
}
