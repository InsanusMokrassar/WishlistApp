package dev.inmo.wishlist.features.ui.wishlist.ui

import android.content.Context
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock

/**
 * `SharedPreferences`-backed [WishlistViewModeStorage] for the Android client.
 *
 * Stores the selected [WishlistViewMode] under [key] (inside the [fileName] preferences file) as its
 * [WishlistViewMode.name] string and reads it back on screen open, so the choice survives an app
 * reopen. Access is guarded by a [SmartRWLocker].
 *
 * @param context Android context used to obtain the [fileName] preferences.
 * @param fileName Name of the private `SharedPreferences` file holding the stored value.
 * @param key Preference key under which the view mode name is stored.
 */
class SharedPreferencesWishlistViewModeStorage(
    context: Context,
    fileName: String = "wishlist.items",
    private val key: String = "viewMode"
) : WishlistViewModeStorage {
    private val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val locker = SmartRWLocker()

    override suspend fun getViewMode(): WishlistViewMode? = locker.withReadAcquire {
        val stored = prefs.getString(key, null) ?: return@withReadAcquire null
        WishlistViewMode.entries.firstOrNull { it.name == stored }
    }

    override suspend fun saveViewMode(mode: WishlistViewMode) {
        locker.withWriteLock {
            prefs.edit().putString(key, mode.name).apply()
        }
    }
}
