package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Browser `localStorage`-backed [WishlistViewModeStorage] for the JS client.
 *
 * Stores the selected [WishlistViewMode] under [key] as its [WishlistViewMode.name] string and reads
 * it back on screen open, so the chosen presentation survives a page refresh / reopen. Access is
 * guarded by a [SmartRWLocker] so a concurrent suspending reader never steps on a writer.
 *
 * @param key `localStorage` key under which the view mode name is stored.
 */
class LocalStorageWishlistViewModeStorage(
    private val key: String = "wishlist.items.viewMode"
) : WishlistViewModeStorage {
    private val locker = SmartRWLocker()

    override suspend fun getViewMode(): WishlistViewMode? = locker.withReadAcquire {
        val stored = localStorage[key] ?: return@withReadAcquire null
        WishlistViewMode.entries.firstOrNull { it.name == stored }
    }

    override suspend fun saveViewMode(mode: WishlistViewMode) {
        locker.withWriteLock {
            localStorage[key] = mode.name
        }
    }
}
