package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the "copy item to my wishlist" target-picker screen.
 *
 * Loads the caller's own wishlists into [targetsState] so the user can pick a destination, then
 * deep-copies the source item (identified by the screen config) into the selected wishlist via the
 * server. Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistItemCopyViewModel(
    private val node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistItemCopyViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _targetsState = MutableRedeliverStateFlow<List<RegisteredWishlist>>(emptyList())

    /** Caller-owned wishlists offered as copy targets. */
    val targetsState = _targetsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(true)

    /** `true` while loading the target list or performing a copy. */
    val loadingState = _loadingState.asStateFlow()

    private val _errorState = MutableRedeliverStateFlow(false)

    /** `true` when the most recent copy attempt failed. */
    val errorState = _errorState.asStateFlow()

    init {
        scope.launchLoggingDropExceptions {
            _loadingState.value = true
            try {
                _targetsState.value = model.getMyWishlists()
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Copies the source item into [targetWishlistId] and, on success, leaves the picker.
     *
     * @param targetWishlistId Caller-owned wishlist chosen as the destination.
     */
    fun onSelectTarget(targetWishlistId: WishlistId) {
        scope.launchLoggingDropExceptions {
            _errorState.value = false
            _loadingState.value = true
            val result = try {
                model.copyItemToWishlist(
                    sourceItemId = node.config.sourceItemId,
                    sourceWishlistId = node.config.sourceWishlistId,
                    targetWishlistId = targetWishlistId
                )
            } finally {
                _loadingState.value = false
            }
            if (result != null) {
                interactor.onCopied(node)
            } else {
                _errorState.value = true
            }
        }
    }

    /** Delegates to [WishlistItemCopyViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }
}
