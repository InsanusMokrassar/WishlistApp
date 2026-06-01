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
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the wishlists list screen.
 *
 * Loads the authenticated caller's wishlists on init and delegates navigation
 * side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistsListViewModel(
    private val node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistsListViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _wishlistsState = MutableRedeliverStateFlow<List<RegisteredWishlist>>(emptyList())

    /** Current list of wishlists owned by the authenticated caller. */
    val wishlistsState = _wishlistsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    /**
     * Owner whose wishlists are shown, or `null` when displaying the caller's own list.
     * `null` hides the grid-view button (no concrete user to open the grid for).
     */
    val targetUserId: UserId? = node.config.userId

    private val _profileUserIdState = MutableRedeliverStateFlow<UserId?>(node.config.userId)

    /**
     * User whose profile the "Profile" button opens: the displayed owner when browsing a concrete
     * user, otherwise the authenticated caller (own wishlists). `null` hides the button (anonymous
     * viewing own list).
     */
    val profileUserIdState = _profileUserIdState.asStateFlow()

    private val _userNameState = MutableRedeliverStateFlow<String?>(null)

    /**
     * Display name of the user whose wishlists are shown (the browsed owner, or the caller for the
     * own list), used to build the personalized title. `null` until resolved or when no user could
     * be resolved (anonymous own list) — the view then falls back to the generic title.
     */
    val userNameState = _userNameState.asStateFlow()

    private val _isOwnerState = MutableRedeliverStateFlow(false)

    /**
     * `true` when the authenticated caller owns the displayed list and may create wishlists in it:
     * either browsing their own list ([targetUserId] is `null`) or browsing themselves by id.
     * `false` for anonymous callers and when browsing another user — hides the "New Wishlist" button.
     */
    val isOwnerState = _isOwnerState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadWishlists()
        }
    }

    /** Reloads the wishlist list from the server. */
    private suspend fun loadWishlists() {
        _loadingState.value = true
        try {
            val targetUserId = node.config.userId
            val currentUserId = model.getCurrentUserId()
            _wishlistsState.value = if (targetUserId == null) {
                model.getMyWishlists()
            } else {
                model.getUserWishlists(targetUserId)
            }
            val profileUserId = targetUserId ?: currentUserId
            _profileUserIdState.value = profileUserId
            _userNameState.value = profileUserId?.let { model.getUserName(it) }
            _isOwnerState.value = currentUserId != null && (targetUserId == null || targetUserId == currentUserId)
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Opens the profile of the user whose wishlists are displayed (the browsed owner, or the
     * caller for the own list). No-op when no user could be resolved (anonymous own list).
     */
    fun onShowProfile() {
        val userId = _profileUserIdState.value ?: return
        scope.launchLoggingDropExceptions { interactor.onShowUser(node, userId) }
    }

    /**
     * Delegates to [WishlistsListViewInteractor.onWishlistSelected].
     *
     * @param wishlistId Identifier of the wishlist the user tapped.
     */
    fun onWishlistSelected(wishlistId: WishlistId) {
        scope.launchLoggingDropExceptions { interactor.onWishlistSelected(node, wishlistId) }
    }

    /** Delegates to [WishlistsListViewInteractor.onCreateWishlist]. */
    fun onCreateWishlist() {
        scope.launchLoggingDropExceptions { interactor.onCreateWishlist(node) }
    }

    /** Delegates to [WishlistsListViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /**
     * Opens the grid presentation of the displayed user's wishlists.
     * No-op when [targetUserId] is `null` (the caller's own list).
     */
    fun onShowUserWishlists() {
        val userId = targetUserId ?: return
        scope.launchLoggingDropExceptions { interactor.onShowUserWishlists(node, userId) }
    }
}
