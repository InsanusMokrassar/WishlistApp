package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the "all items" presentation of a user's wishlists.
 *
 * Loads every item across all wishlists owned by [UserWishlistsViewConfig.userId] on init and on
 * node resume, flattening them into a single list, and delegates navigation side-effects to
 * [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class UserWishlistsViewModel(
    private val node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: UserWishlistsViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _itemsState = MutableRedeliverStateFlow<List<RegisteredWishlistItem>>(emptyList())

    /** All items across every wishlist of the target user, flattened into one list. */
    val itemsState = _itemsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                _itemsState.value = model.getUserWishlists(node.config.userId)
                    .flatMap { model.getWishlistItems(it.id) }
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Delegates to [UserWishlistsViewInteractor.onItemSelected].
     *
     * @param item Item whose detail screen should be opened.
     */
    fun onItemSelected(item: RegisteredWishlistItem) {
        scope.launchLoggingDropExceptions { interactor.onItemSelected(node, item.id, item.wishlistId) }
    }

    /** Delegates to [UserWishlistsViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }
}
