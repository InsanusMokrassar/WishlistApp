package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.e
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the wishlists list screen.
 *
 * Loads the authenticated caller's wishlists on init and delegates navigation
 * side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 * @param dispatcher UI dispatcher for auth and list lifecycle work.
 */
class WishlistsListViewModel(
    private val node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistsListViewInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel<ViewConfig>(node) {
    /** UI-confined lifecycle child scope used for auth-triggered list loading. */
    private val workScope = CoroutineScope(scope.coroutineContext + dispatcher)
    private val _wishlistsState = MutableRedeliverStateFlow<List<WishlistsFeatureWishlist>>(emptyList())

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

    /**
     * `true` when the authenticated caller owns the displayed list and may create wishlists in it:
     * either browsing their own list ([targetUserId] is `null`) or browsing themselves by id.
     * `false` for anonymous callers and when browsing another user — hides the "New Wishlist" button.
     * Derived reactively from [WishlistsModel.isOwnerFlow], so it self-corrects once the cold-start
     * `getMe()` round-trip completes and on later login/logout (PR #31 F2).
     */
    val isOwnerState: StateFlow<Boolean> =
        model.isOwnerFlow(targetUserId).stateIn(scope, SharingStarted.Eagerly, false)

    /** Monotonic token that prevents cancelled or noncooperative loads from publishing stale private data. */
    private var loadGeneration = 0L

    init {
        val reloadTriggers = if (targetUserId == null) {
            merge(model.userAuthorisedState.map { }, node.onResumeFlow)
        } else {
            merge(flowOf(Unit), node.onResumeFlow)
        }
        workScope.launch {
            reloadTriggers.collectLatest {
                try {
                    loadWishlists()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Throwable) {
                    runCatching {
                        KSLog.e("WishlistsListViewModel", "Failed to load ${loadTargetDescription()}", exception)
                    }
                }
            }
        }
    }

    /** Reloads the wishlist list from the server. */
    private suspend fun loadWishlists() {
        val generation = ++loadGeneration
        val configuredUserId = targetUserId
        if (configuredUserId == null && !model.userAuthorisedState.value) {
            clearOwnListState(generation)
            return
        }
        _loadingState.value = true
        try {
            val wishlists = if (configuredUserId == null) {
                model.getMyWishlists()
            } else {
                model.getUserWishlists(configuredUserId)
            }
            if (!canPublish(generation, configuredUserId)) return
            _wishlistsState.value = wishlists
            val profileUserId = configuredUserId ?: model.currentUserIdFlow.value
            if (!canPublish(generation, configuredUserId)) return
            _profileUserIdState.value = profileUserId
            val userName = profileUserId?.let { model.getUserName(it) }
            if (!canPublish(generation, configuredUserId)) return
            _userNameState.value = userName
        } finally {
            if (generation == loadGeneration) _loadingState.value = false
        }
    }

    /** Clears caller-derived state immediately after logout or anonymous own-list startup. */
    private fun clearOwnListState(generation: Long) {
        if (generation != loadGeneration) return
        _wishlistsState.value = emptyList()
        _profileUserIdState.value = null
        _userNameState.value = null
        _loadingState.value = false
    }

    /** Confirms that a completed load still belongs to the visible target and current authorization state. */
    private fun canPublish(generation: Long, targetUserId: UserId?): Boolean =
        generation == loadGeneration && (targetUserId != null || model.userAuthorisedState.value)

    /** Describes the active list target without exposing caller-derived wishlist data in error logs. */
    private fun loadTargetDescription(): String = targetUserId?.let { "public wishlist list for user $it" } ?: "own wishlist list"

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
