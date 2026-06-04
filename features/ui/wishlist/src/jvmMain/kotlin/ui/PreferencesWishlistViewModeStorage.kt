package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import java.util.prefs.Preferences

/**
 * `java.util.prefs.Preferences`-backed [WishlistViewModeStorage] for the JVM (desktop) client.
 *
 * Stores the selected [WishlistViewMode] under [key] (inside the [nodeName] preferences node) as its
 * [WishlistViewMode.name] string and reads it back on screen open, so the choice survives an app
 * reopen. Access is guarded by a [SmartRWLocker].
 *
 * @param nodeName User-root preferences node holding the stored value.
 * @param key Preference key under which the view mode name is stored.
 */
class PreferencesWishlistViewModeStorage(
    nodeName: String = "wishlist/items",
    private val key: String = "viewMode"
) : WishlistViewModeStorage {
    private val preferences: Preferences = Preferences.userRoot().node(nodeName)
    private val locker = SmartRWLocker()

    override suspend fun getViewMode(): WishlistViewMode? = locker.withReadAcquire {
        val stored = preferences.get(key, null) ?: return@withReadAcquire null
        WishlistViewMode.entries.firstOrNull { it.name == stored }
    }

    override suspend fun saveViewMode(mode: WishlistViewMode) {
        locker.withWriteLock {
            preferences.put(key, mode.name)
            preferences.flush()
        }
    }
}
