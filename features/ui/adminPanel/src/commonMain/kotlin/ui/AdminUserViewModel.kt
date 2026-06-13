package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the admin user detail screen.
 *
 * Loads the user and their wishlists on init and on each resume.
 * Delegates navigation side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminUserViewModel(
    private val node: NavigationNode<AdminUserViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminUserViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _userState = MutableRedeliverStateFlow<RegisteredUser?>(null)

    /** The loaded user, `null` while loading or on error. */
    val userState = _userState.asStateFlow()

    private val _wishlistsState = MutableRedeliverStateFlow<List<RegisteredWishlist>>(emptyList())

    /** Wishlists owned by the loaded user. */
    val wishlistsState = _wishlistsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUser()
        }
    }

    private suspend fun loadUser() {
        _loadingState.value = true
        try {
            _userState.value = model.getUserById(node.config.userId)
            _wishlistsState.value = model.getWishlistsByUser(node.config.userId)
        } finally {
            _loadingState.value = false
        }
    }

    /** Delegates to [AdminUserViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [AdminUserViewInteractor.onEditUser]. */
    fun onEditUser() {
        scope.launchLoggingDropExceptions { interactor.onEditUser(node) }
    }

    /**
     * Delegates to [AdminUserViewInteractor.onOpenWishlist].
     *
     * @param wishlistId Identifier of the wishlist the admin tapped.
     */
    fun onOpenWishlist(wishlistId: WishlistId) {
        scope.launchLoggingDropExceptions { interactor.onOpenWishlist(node, wishlistId) }
    }

    /** Delegates to [AdminUserViewInteractor.onAddWishlist]. */
    fun onAddWishlist() {
        scope.launchLoggingDropExceptions { interactor.onAddWishlist(node, node.config.userId) }
    }
}
