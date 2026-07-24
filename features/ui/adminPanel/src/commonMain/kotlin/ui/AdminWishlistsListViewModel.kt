package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the admin wishlists list screen.
 *
 * Loads all wishlists on init and on each resume. Delegates navigation side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminWishlistsListViewModel(
    private val node: NavigationNode<AdminWishlistsListViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminWishlistsListViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _wishlistsState = MutableRedeliverStateFlow<List<AdminWishlist>>(emptyList())

    /** Current list of all wishlists. */
    val wishlistsState = _wishlistsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadWishlists()
        }
    }

    private suspend fun loadWishlists() {
        _loadingState.value = true
        try {
            _wishlistsState.value = model.getAllWishlists()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Delegates to [AdminWishlistsListViewInteractor.onWishlistSelected].
     *
     * @param wishlistId Identifier of the wishlist the admin tapped.
     */
    fun onWishlistSelected(wishlistId: WishlistId) {
        scope.launchLoggingDropExceptions { interactor.onWishlistSelected(node, wishlistId) }
    }

    /** Delegates to [AdminWishlistsListViewInteractor.onCreateWishlist]. */
    fun onCreateWishlist() {
        scope.launchLoggingDropExceptions { interactor.onCreateWishlist(node) }
    }
}
