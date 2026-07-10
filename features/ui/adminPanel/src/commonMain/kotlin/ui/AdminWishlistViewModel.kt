package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the admin wishlist detail screen.
 *
 * Loads the wishlist and its items on init and on each resume.
 * Items are shown inline; deletion is handled on this screen.
 * Delegates navigation side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminWishlistViewModel(
    private val node: NavigationNode<AdminWishlistViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminWishlistViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _wishlistState = MutableRedeliverStateFlow<AdminWishlist?>(null)

    /** The loaded wishlist, `null` while loading or on error. */
    val wishlistState = _wishlistState.asStateFlow()

    private val _itemsState = MutableRedeliverStateFlow<List<AdminWishlistItem>>(emptyList())

    /** Items belonging to the loaded wishlist. */
    val itemsState = _itemsState.asStateFlow()

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
            _wishlistState.value = model.getWishlistById(node.config.wishlistId)
            _itemsState.value = model.getItemsByWishlist(node.config.wishlistId)
        } finally {
            _loadingState.value = false
        }
    }

    /** Delegates to [AdminWishlistViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [AdminWishlistViewInteractor.onEditWishlist]. */
    fun onEditWishlist() {
        scope.launchLoggingDropExceptions { interactor.onEditWishlist(node) }
    }

    /** Delegates to [AdminWishlistViewInteractor.onAddItem]. */
    fun onAddItem() {
        scope.launchLoggingDropExceptions { interactor.onAddItem(node, node.config.wishlistId) }
    }

    /**
     * Delegates to [AdminWishlistViewInteractor.onEditItem].
     *
     * @param itemId Item to edit.
     */
    fun onEditItem(itemId: WishlistItemId) {
        scope.launchLoggingDropExceptions { interactor.onEditItem(node, itemId, node.config.wishlistId) }
    }

    /**
     * Deletes the given item and reloads the list.
     *
     * @param itemId Item to delete.
     */
    fun onDeleteItem(itemId: WishlistItemId) {
        scope.launchLoggingDropExceptions {
            _loadingState.value = true
            try {
                model.deleteWishlistItem(itemId)
                _itemsState.value = model.getItemsByWishlist(node.config.wishlistId)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
