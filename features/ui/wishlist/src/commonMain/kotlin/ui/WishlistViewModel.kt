package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the wishlist detail screen.
 *
 * Loads the wishlist and its items on init. Exposes [isOwnerState] which is `true`
 * when the authenticated caller is the wishlist owner — used to show edit controls.
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistViewModel(
    private val node: NavigationNode<WishlistViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _wishlistState = MutableRedeliverStateFlow<RegisteredWishlist?>(null)

    /** The loaded wishlist, `null` while loading or on error. */
    val wishlistState = _wishlistState.asStateFlow()

    private val _itemsState = MutableRedeliverStateFlow<List<RegisteredWishlistItem>>(emptyList())

    /** Items belonging to the loaded wishlist. */
    val itemsState = _itemsState.asStateFlow()

    private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)

    /**
     * `true` when the authenticated caller is the wishlist owner.
     * Derived from [_wishlistState] and [_currentUserIdState].
     */
    val isOwnerState: StateFlow<Boolean> = combine(_wishlistState, _currentUserIdState) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId == userId
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadWishlist()
        }
    }

    private suspend fun loadWishlist() {
        _loadingState.value = true
        val wishlist = try {
            _currentUserIdState.value = model.getCurrentUserId()
            val loaded = model.getWishlist(node.config.wishlistId)
            _wishlistState.value = loaded
            _itemsState.value = model.getWishlistItems(node.config.wishlistId)
            loaded
        } finally {
            _loadingState.value = false
        }
        // Wishlist may have been deleted (here or from the edit screen) — leave the screen
        // automatically when it no longer exists, matching a plain back navigation.
        if (wishlist == null) {
            interactor.onBack(node)
        }
    }

    /** Delegates to [WishlistViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [WishlistViewInteractor.onEditWishlist]. Requires ownership. */
    fun onEditWishlist() {
        scope.launchLoggingDropExceptions { interactor.onEditWishlist(node) }
    }

    /**
     * Delegates to [WishlistViewInteractor.onViewItem].
     *
     * Both owners and non-owners open the read-only view first; owners may proceed to edit from there.
     *
     * @param itemId Item to view.
     */
    fun onViewItem(itemId: WishlistItemId) {
        scope.launchLoggingDropExceptions { interactor.onViewItem(node, itemId) }
    }

    /** Delegates to [WishlistViewInteractor.onAddItem]. */
    fun onAddItem() {
        scope.launchLoggingDropExceptions { interactor.onAddItem(node) }
    }
}
