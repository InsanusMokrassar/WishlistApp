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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the wishlist item read-only view screen.
 *
 * Loads the item identified by [WishlistItemViewConfig.wishlistItemId] and the parent wishlist.
 * Exposes [isOwnerState] so the view can conditionally render an "Edit" button.
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistItemViewModel(
    private val node: NavigationNode<WishlistItemViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistItemViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _itemState = MutableRedeliverStateFlow<RegisteredWishlistItem?>(null)

    /** The loaded item, `null` while loading or when not found. */
    val itemState = _itemState.asStateFlow()

    private val _wishlistState = MutableRedeliverStateFlow<RegisteredWishlist?>(null)
    private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)

    /**
     * `true` when the authenticated caller is the parent wishlist owner.
     * Controls visibility of the Edit button.
     */
    val isOwnerState: StateFlow<Boolean> = combine(_wishlistState, _currentUserIdState) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId == userId
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            val item = try {
                _currentUserIdState.value = model.getCurrentUserId()
                _wishlistState.value = model.getWishlist(node.config.wishlistId)
                model.getWishlistItems(node.config.wishlistId)
                    .find { it.id == node.config.wishlistItemId }
                    .also { _itemState.value = it }
            } finally {
                _loadingState.value = false
            }
            // Item may have been deleted (here or from the edit screen) — leave the screen
            // automatically when it no longer exists, matching a plain back navigation.
            if (item == null) {
                interactor.onBack(node)
            }
        }
    }

    /** Delegates to [WishlistItemViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [WishlistItemViewInteractor.onEditItem]. Only meaningful when [isOwnerState] is `true`. */
    fun onEditItem() {
        scope.launchLoggingDropExceptions { interactor.onEditItem(node) }
    }
}
