package dev.inmo.wishlist.features.ui.wishlist.ui

/**
 * Presentation mode for the collections of wishlist items shown on the wishlist detail and all-items
 * screens.
 *
 * Lets the user switch between the compact single-column row layout and a multi-column card grid.
 */
enum class WishlistViewMode {
    /** Items are shown as single-column rows (the default, compact presentation). */
    List,

    /** Items are shown as cards arranged in a responsive multi-column grid. */
    Grid
}
