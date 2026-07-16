package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import kotlinx.coroutines.flow.StateFlow

/**
 * Outside-world data the sidebar needs.
 *
 * Composes the existing wishlist and booking UI models, and the users UI model (for the root-only
 * admin check), so the sidebar never talks to feature clients directly. The concrete implementation
 * is registered in this feature's [dev.inmo.wishlist.features.ui.sidebar.Plugin].
 */
interface SidebarModel {
    /**
     * Reactive id of the authenticated caller ("me"), or `null` when anonymous / not yet resolved.
     * The sidebar swaps between the profile row and the login control on this flow.
     */
    val currentUserIdFlow: StateFlow<UserId?>

    /**
     * Reactive flag: `true` while the authenticated caller is the `root` user — the only identity
     * permitted to see the sidebar's admin-panel entry point.
     *
     * Delegates to [dev.inmo.wishlist.features.ui.users.ui.UsersModel.isCurrentUserRootFlow]; `false`
     * while anonymous or not yet resolved.
     */
    val isCurrentUserRootFlow: StateFlow<Boolean>

    /**
     * Returns every wishlist owned by the authenticated caller, used to pin the caller's own lists
     * below the primary navigation.
     *
     * @return Owned wishlists; empty when none or when anonymous.
     */
    suspend fun getMyWishlists(): List<RegisteredWishlist>

    /**
     * Counts the items the caller has reserved to gift, shown as the live badge on the Reserved item.
     *
     * @return Number of reserved items; `0` when none or when anonymous.
     */
    suspend fun getReservedCount(): Int

    /**
     * Resolves the display name of [userId] for the profile row.
     *
     * @param userId User whose name to resolve.
     * @return Username string, or `null` when unknown.
     */
    suspend fun getUserName(userId: UserId): String?
}
