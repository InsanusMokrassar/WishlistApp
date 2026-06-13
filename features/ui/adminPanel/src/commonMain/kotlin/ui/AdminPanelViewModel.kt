package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * ViewModel for the admin panel dashboard screen.
 *
 * Delegates navigation to [interactor]. No data loading — dashboard is purely navigational.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source (unused on dashboard, injected for consistency).
 * @param interactor Navigation delegate for this screen.
 */
class AdminPanelViewModel(
    private val node: NavigationNode<AdminPanelViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminPanelViewInteractor
) : ViewModel<ViewConfig>(node) {

    /** Called when the user taps the Users section button. */
    fun onOpenUsers() {
        scope.launchLoggingDropExceptions { interactor.onOpenUsers(node) }
    }

    /** Called when the user taps the Wishlists section button. */
    fun onOpenWishlists() {
        scope.launchLoggingDropExceptions { interactor.onOpenWishlists(node) }
    }
}
