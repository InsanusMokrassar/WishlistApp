package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [resolveActiveSectionForStack] — the pure main-chain-stack-to-[SidebarSection] mapping
 * `SidebarViewModel.resolveActiveSection()` delegates to. No [dev.inmo.navigation.core.NavigationNode]
 * or [dev.inmo.navigation.core.NavigationChain] involved: fixtures are plain [ViewConfig] instances.
 */
class SidebarViewModelTest {

    /** An empty stack (no navigation yet, or main chain not found) resolves to [SidebarSection.None]. */
    @Test
    fun emptyStackReturnsNone() {
        assertEquals(SidebarSection.None, resolveActiveSectionForStack(emptyList()))
    }

    /** [WishlistsListViewConfig] with a `null` `userId` (the caller's own lists) maps to [SidebarSection.MyLists]. */
    @Test
    fun ownWishlistsListMapsToMyLists() {
        val stack = listOf<ViewConfig>(WishlistsListViewConfig(userId = null))
        assertEquals(SidebarSection.MyLists, resolveActiveSectionForStack(stack))
    }

    /**
     * [WishlistsListViewConfig] with a non-null `userId` (someone else's lists, reached from Discover)
     * is deliberately NOT [SidebarSection.MyLists] — with nothing else on the stack it falls through to
     * [SidebarSection.None].
     */
    @Test
    fun otherUsersWishlistsListDoesNotMapToMyLists() {
        val stack = listOf<ViewConfig>(WishlistsListViewConfig(userId = UserId(1L)))
        assertEquals(SidebarSection.None, resolveActiveSectionForStack(stack))
    }

    /** [UsersListViewConfig] maps to [SidebarSection.Discover]. */
    @Test
    fun usersListMapsToDiscover() {
        val stack = listOf<ViewConfig>(UsersListViewConfig())
        assertEquals(SidebarSection.Discover, resolveActiveSectionForStack(stack))
    }

    /** [MyPresentsBooksViewConfig] maps to [SidebarSection.Reserved]. */
    @Test
    fun myPresentsBooksMapsToReserved() {
        val stack = listOf<ViewConfig>(MyPresentsBooksViewConfig())
        assertEquals(SidebarSection.Reserved, resolveActiveSectionForStack(stack))
    }

    /** [UserEditViewConfig] maps to [SidebarSection.Settings]. */
    @Test
    fun userEditMapsToSettings() {
        val stack = listOf<ViewConfig>(UserEditViewConfig(UserId(1L)))
        assertEquals(SidebarSection.Settings, resolveActiveSectionForStack(stack))
    }

    /**
     * [AdminPanelViewConfig] maps to [SidebarSection.Admin] — the mapping added by issue #66. Covers the
     * one genuinely new branch this task adds to the resolver.
     */
    @Test
    fun adminPanelMapsToAdmin() {
        val stack = listOf<ViewConfig>(AdminPanelViewConfig())
        assertEquals(SidebarSection.Admin, resolveActiveSectionForStack(stack))
    }

    /**
     * Scans from the topmost entry down: a [UserEditViewConfig] pushed on top of [UsersListViewConfig]
     * resolves to [SidebarSection.Settings] (the screen actually showing), not [SidebarSection.Discover]
     * (the screen underneath). Regression guard for the refactor that extracted this function out of
     * `resolveActiveSection()` — the reversed-scan order must be preserved exactly.
     */
    @Test
    fun topmostEntryWinsOverEntriesBelowIt() {
        val stack = listOf<ViewConfig>(UsersListViewConfig(), UserEditViewConfig(UserId(1L)))
        assertEquals(SidebarSection.Settings, resolveActiveSectionForStack(stack))
    }

    /**
     * Same top-wins semantics for the new [AdminPanelViewConfig] branch: pushed above
     * [MyPresentsBooksViewConfig], it resolves to [SidebarSection.Admin], not [SidebarSection.Reserved].
     */
    @Test
    fun adminPanelOnTopWinsOverReservedBelowIt() {
        val stack = listOf<ViewConfig>(MyPresentsBooksViewConfig(), AdminPanelViewConfig())
        assertEquals(SidebarSection.Admin, resolveActiveSectionForStack(stack))
    }
}
