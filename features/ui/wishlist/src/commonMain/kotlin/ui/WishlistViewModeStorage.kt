package dev.inmo.wishlist.features.ui.wishlist.ui

/**
 * Per-client persistent storage for the last [WishlistViewMode] selected by the user on the wishlist
 * detail and all-items screens.
 *
 * The mode is saved whenever the user switches presentation and read back on screen (re)open so the
 * choice survives a page refresh / app reopen instead of resetting to the default. Implementations are
 * platform-specific (JS `localStorage`, JVM `Preferences`, Android `SharedPreferences`) following the
 * same shape as `ServerUrlStorage`.
 */
interface WishlistViewModeStorage {
    /**
     * Reads the last persisted view mode.
     *
     * @return The stored [WishlistViewMode], or `null` when nothing was ever saved or the stored value
     * is unrecognized.
     */
    suspend fun getViewMode(): WishlistViewMode?

    /**
     * Persists [mode] as the latest user-selected view mode.
     *
     * @param mode View mode to store; restored by [getViewMode] on the next screen open.
     */
    suspend fun saveViewMode(mode: WishlistViewMode)
}
