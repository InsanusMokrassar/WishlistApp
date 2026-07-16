package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Side-effecting navigation capability for the sidebar.
 *
 * Every method drives the scaffold's **main** navigation chain (located by
 * [dev.inmo.wishlist.features.common.client.models.MainNavigationChainId]), not the sidebar's own
 * left chain, so selecting an item swaps the content area. The implementation lives in the top-level
 * `client/` module because the feature itself cannot see the configs of every destination.
 */
interface SidebarViewInteractor {
    /**
     * Switches the content area to the caller's own wishlists (the default landing destination).
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onSelectMyLists(node: NavigationNode<SidebarViewConfig, ViewConfig>)

    /**
     * Switches the content area to the people / discover screen.
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onSelectDiscover(node: NavigationNode<SidebarViewConfig, ViewConfig>)

    /**
     * Switches the content area to the caller's reserved items.
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onSelectReserved(node: NavigationNode<SidebarViewConfig, ViewConfig>)

    /**
     * Switches the content area to the caller's account settings (the profile edit screen).
     *
     * @param node Navigation node hosting the sidebar.
     * @param userId Id of the signed-in caller whose settings to open.
     */
    suspend fun onSelectSettings(node: NavigationNode<SidebarViewConfig, ViewConfig>, userId: UserId)

    /**
     * Opens the root-only admin panel dashboard in the content area.
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onSelectAdminPanel(node: NavigationNode<SidebarViewConfig, ViewConfig>)

    /**
     * Opens a specific pinned wishlist in the content area.
     *
     * @param node Navigation node hosting the sidebar.
     * @param wishlistId Wishlist the caller picked from the pinned list.
     */
    suspend fun onSelectWishlist(node: NavigationNode<SidebarViewConfig, ViewConfig>, wishlistId: WishlistId)

    /**
     * Opens the create-wishlist form in the content area.
     *
     * @param node Navigation node hosting the sidebar.
     */
    suspend fun onCreateList(node: NavigationNode<SidebarViewConfig, ViewConfig>)

    /**
     * Opens the public profile of the signed-in caller from the bottom profile row.
     *
     * @param node Navigation node hosting the sidebar.
     * @param userId Id of the signed-in caller whose profile to open.
     */
    suspend fun onOpenProfile(node: NavigationNode<SidebarViewConfig, ViewConfig>, userId: UserId)
}
