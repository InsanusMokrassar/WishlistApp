package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the wishlists list screen.
 *
 * Loads the authenticated caller's wishlists on init and exposes navigation
 * actions to create a new wishlist or open an existing one.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 */
class WishlistsListViewModel(
    private val node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
    private val model: WishlistsModel
) : ViewModel<ViewConfig>(node) {
    private val _wishlistsState = MutableRedeliverStateFlow<List<RegisteredWishlist>>(emptyList())

    /** Current list of wishlists owned by the authenticated caller. */
    val wishlistsState = _wishlistsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadWishlists()
        }
    }

    /** Reloads the wishlist list from the server. */
    private suspend fun loadWishlists() {
        _loadingState.value = true
        try {
            _wishlistsState.value = model.getMyWishlists()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Pushes [WishlistViewConfig] for the selected wishlist onto the navigation chain.
     *
     * @param wishlistId Identifier of the wishlist the user tapped.
     */
    fun onWishlistSelected(wishlistId: WishlistId) {
        scope.launchLoggingDropExceptions {
            node.chain.push(WishlistViewConfig(wishlistId))
        }
    }

    /** Pushes [WishlistEditViewConfig] with null id (create mode) onto the navigation chain. */
    fun onCreateWishlist() {
        scope.launchLoggingDropExceptions {
            node.chain.push(WishlistEditViewConfig(null))
        }
    }
}
