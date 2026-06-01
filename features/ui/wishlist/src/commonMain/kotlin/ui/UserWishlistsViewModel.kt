package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * One section of the all-items screen: a wishlist together with the items it owns.
 *
 * Used to render a separator (the wishlist title) above the wishlist's items.
 *
 * @param wishlist Wishlist this section represents; its title is shown as the separator header.
 * @param items Items belonging to [wishlist], in their stored order.
 */
data class UserWishlistsSection(
    val wishlist: RegisteredWishlist,
    val items: List<RegisteredWishlistItem>
)

/**
 * ViewModel for the "all items" presentation of a user's wishlists.
 *
 * Loads every wishlist owned by [UserWishlistsViewConfig.userId] together with its items on init
 * and on node resume, grouping them into [UserWishlistsSection]s so the view can render each
 * wishlist's items under a separator, and delegates navigation side-effects to [interactor].
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
    private val _sectionsState = MutableRedeliverStateFlow<List<UserWishlistsSection>>(emptyList())

    /**
     * Items of the target user grouped by their wishlist. Wishlists with no items are omitted so
     * the view never renders an empty separator.
     */
    val sectionsState = _sectionsState.asStateFlow()

    private val _userNameState = MutableRedeliverStateFlow<String?>(null)

    /**
     * Display name of the target user, used to build the personalized title. `null` until resolved
     * or when the user is unknown — the view then falls back to the generic title.
     */
    val userNameState = _userNameState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                _userNameState.value = model.getUserName(node.config.userId)
                _sectionsState.value = model.getUserWishlists(node.config.userId)
                    .map { wishlist -> UserWishlistsSection(wishlist, model.getWishlistItems(wishlist.id)) }
                    .filter { it.items.isNotEmpty() }
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

    /** Opens the target user's public profile via [UserWishlistsViewInteractor.onOpenProfile]. */
    fun onOpenProfile() {
        scope.launchLoggingDropExceptions { interactor.onOpenProfile(node, node.config.userId) }
    }

    /** Download URL of image [id], for platforms that render directly from a URL (JS). */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /** Raw bytes of image [id], for platforms that decode images locally (JVM/Android). */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}
