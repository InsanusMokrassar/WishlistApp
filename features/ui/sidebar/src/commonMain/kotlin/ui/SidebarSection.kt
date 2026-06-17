package dev.inmo.wishlist.features.ui.sidebar.ui

/**
 * Top-level navigation destinations surfaced as primary items in the sidebar.
 *
 * Used both to render the active-item highlight and to label which content root the main chain is
 * currently showing. [None] means the main chain is on a screen that no primary item owns (e.g. a
 * pushed detail or edit form reached from elsewhere).
 */
enum class SidebarSection {
    /** The signed-in caller's own wishlists — the default landing destination. */
    MyLists,

    /** Browse other people and their public wishlists. */
    Discover,

    /** Items the caller has reserved to gift. */
    Reserved,

    /** Account settings for the signed-in caller. */
    Settings,

    /** No primary section owns the current main-chain screen. */
    None
}
