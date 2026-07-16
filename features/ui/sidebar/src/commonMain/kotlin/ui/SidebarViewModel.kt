package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the Calm Studio sidebar.
 *
 * Holds the data the sidebar renders (signed-in caller, pinned lists, live reserved count) and the
 * currently-active primary section. Data is reloaded on first show, on every login/logout, and on
 * every navigation change (so a freshly created list or reservation shows up immediately). All
 * navigation is delegated to [interactor], which drives the scaffold's main chain.
 *
 * @param node Navigation node owning the sidebar (lives in the scaffold's left slot).
 * @param model Sidebar data source.
 * @param interactor Navigation delegate that swaps the main content area.
 */
class SidebarViewModel(
    private val node: NavigationNode<SidebarViewConfig, ViewConfig>,
    private val model: SidebarModel,
    private val interactor: SidebarViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val rootChain = node.chain.rootChain()

    /** Reactive id of the signed-in caller, or `null` when anonymous. */
    val currentUserIdState: StateFlow<UserId?> = model.currentUserIdFlow

    /**
     * Reactive flag: `true` while the signed-in caller is `root`. Gates the sidebar's admin-panel
     * entry point, which is fully absent (not merely disabled) for non-root/anonymous callers.
     */
    val isRootState: StateFlow<Boolean> = model.isCurrentUserRootFlow

    private val _userNameState = MutableRedeliverStateFlow<String?>(null)

    /** Display name of the signed-in caller, shown in the profile row; `null` until resolved. */
    val userNameState = _userNameState.asStateFlow()

    private val _myListsState = MutableRedeliverStateFlow<List<RegisteredWishlist>>(emptyList())

    /** The caller's own wishlists, pinned below the primary navigation. */
    val myListsState = _myListsState.asStateFlow()

    private val _reservedCountState = MutableRedeliverStateFlow(0)

    /** Number of items the caller has reserved, shown as the live Reserved badge. */
    val reservedCountState = _reservedCountState.asStateFlow()

    private val _activeSectionState = MutableRedeliverStateFlow(SidebarSection.MyLists)

    /** The primary section the main chain is currently showing, used for the active highlight. */
    val activeSectionState = _activeSectionState.asStateFlow()

    init {
        merge(
            flowOf(Unit),
            model.currentUserIdFlow.map { },
            rootChain.changesInSubTreeFlow().map { }
        ).conflate().subscribeLoggingDropExceptions(scope) {
            reload()
            _activeSectionState.value = resolveActiveSection()
        }
    }

    /** Reloads the pinned lists, reserved count and caller name for the current auth state. */
    private suspend fun reload() {
        val userId = model.currentUserIdFlow.value
        if (userId == null) {
            _myListsState.value = emptyList()
            _reservedCountState.value = 0
            _userNameState.value = null
            return
        }
        _myListsState.value = model.getMyWishlists()
        _reservedCountState.value = model.getReservedCount()
        _userNameState.value = model.getUserName(userId)
    }

    /**
     * Maps the main chain's current stack to the primary section that owns it, scanning from the
     * topmost node down so a pushed detail resolves to the section it was opened from.
     */
    private fun resolveActiveSection(): SidebarSection {
        val mainChain: NavigationChain<ViewConfig> = rootChain.findInSubTree(MainNavigationChainId)
            ?: return SidebarSection.None
        return resolveActiveSectionForStack(mainChain.stackFlow.value.map { it.config })
    }

    /** Switches to the caller's own wishlists; no-op when anonymous. */
    fun onSelectMyLists() {
        if (model.currentUserIdFlow.value == null) return
        scope.launchLoggingDropExceptions { interactor.onSelectMyLists(node) }
    }

    /** Delegates to [SidebarViewInteractor.onSelectDiscover]. */
    fun onSelectDiscover() {
        scope.launchLoggingDropExceptions { interactor.onSelectDiscover(node) }
    }

    /** Switches to the caller's reserved gifts; no-op when anonymous. */
    fun onSelectReserved() {
        if (model.currentUserIdFlow.value == null) return
        scope.launchLoggingDropExceptions { interactor.onSelectReserved(node) }
    }

    /** Opens account settings for the signed-in caller; no-op when anonymous. */
    fun onSelectSettings() {
        val userId = model.currentUserIdFlow.value ?: return
        scope.launchLoggingDropExceptions { interactor.onSelectSettings(node, userId) }
    }

    /** Opens the root-only admin panel dashboard; no-op when the caller is not root. */
    fun onSelectAdminPanel() {
        if (!isRootState.value) return
        scope.launchLoggingDropExceptions { interactor.onSelectAdminPanel(node) }
    }

    /**
     * Opens the pinned wishlist [wishlistId] in the content area.
     *
     * @param wishlistId Wishlist the caller picked from the pinned list.
     */
    fun onSelectWishlist(wishlistId: WishlistId) {
        scope.launchLoggingDropExceptions { interactor.onSelectWishlist(node, wishlistId) }
    }

    /** Delegates to [SidebarViewInteractor.onCreateList]. */
    fun onCreateList() {
        scope.launchLoggingDropExceptions { interactor.onCreateList(node) }
    }

    /** Opens the signed-in caller's profile; no-op when anonymous. */
    fun onOpenProfile() {
        val userId = model.currentUserIdFlow.value ?: return
        scope.launchLoggingDropExceptions { interactor.onOpenProfile(node, userId) }
    }
}

/**
 * Pure mapping from a main-chain stack (as ordered by [NavigationChain.stackFlow], bottom-most first)
 * to the [SidebarSection] that owns the topmost recognized screen.
 *
 * Scans from the topmost entry down so a pushed detail (e.g. a screen reached from elsewhere) still
 * resolves to the section it was opened from. Returns [SidebarSection.None] when no entry in the stack
 * maps to a primary section. Extracted as a standalone, [NavigationNode]-free function so the mapping
 * can be unit-tested with plain [ViewConfig] fixtures instead of a live navigation chain.
 *
 * @param configs Main-chain [ViewConfig]s, bottom-most first.
 * @return The [SidebarSection] the topmost recognized entry belongs to, or [SidebarSection.None].
 */
internal fun resolveActiveSectionForStack(configs: List<ViewConfig>): SidebarSection {
    for (cfg in configs.asReversed()) {
        val section = when (cfg) {
            is WishlistsListViewConfig -> if (cfg.userId == null) SidebarSection.MyLists else null
            is UsersListViewConfig -> SidebarSection.Discover
            is MyPresentsBooksViewConfig -> SidebarSection.Reserved
            is UserEditViewConfig -> SidebarSection.Settings
            is AdminPanelViewConfig -> SidebarSection.Admin
            else -> null
        }
        if (section != null) return section
    }
    return SidebarSection.None
}
