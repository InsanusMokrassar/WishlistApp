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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the wishlist detail screen.
 *
 * Loads the wishlist and its items on init. Exposes [isOwnerState] which is `true`
 * when the authenticated caller is the wishlist owner — used to show edit controls.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 */
class WishlistViewModel(
    private val node: NavigationNode<WishlistViewConfig, ViewConfig>,
    private val model: WishlistsModel
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
        try {
            _currentUserIdState.value = model.getCurrentUserId()
            _wishlistState.value = model.getWishlist(node.config.wishlistId)
            _itemsState.value = model.getWishlistItems(node.config.wishlistId)
        } finally {
            _loadingState.value = false
        }
    }

    /** Pops the current node from its chain, returning to the wishlists list. */
    fun onBack() {
        scope.launchLoggingDropExceptions { node.chain.pop() }
    }

    /** Pushes [WishlistEditViewConfig] for this wishlist onto the chain. Requires ownership. */
    fun onEditWishlist() {
        scope.launchLoggingDropExceptions {
            node.chain.push(WishlistEditViewConfig(node.config.wishlistId))
        }
    }

    /**
     * Pushes [WishlistItemEditViewConfig] for item [itemId] onto the chain.
     *
     * @param itemId Item to edit.
     */
    fun onEditItem(itemId: WishlistItemId) {
        scope.launchLoggingDropExceptions {
            node.chain.push(WishlistItemEditViewConfig(itemId, node.config.wishlistId))
        }
    }

    /** Pushes [WishlistItemEditViewConfig] in create mode (null item id) onto the chain. */
    fun onAddItem() {
        scope.launchLoggingDropExceptions {
            node.chain.push(WishlistItemEditViewConfig(null, node.config.wishlistId))
        }
    }
}
