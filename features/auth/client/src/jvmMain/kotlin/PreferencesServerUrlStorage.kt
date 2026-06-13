package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import java.util.prefs.Preferences

class PreferencesServerUrlStorage(
    nodeName: String = "wishlist/serverAddress",
    private val key: String = "url"
) : ServerUrlStorage {
    private val preferences: Preferences = Preferences.userRoot().node(nodeName)
    private val locker = SmartRWLocker()

    override suspend fun getServerUrl(): String? = locker.withReadAcquire {
        preferences.get(key, null)
    }

    override suspend fun saveServerUrl(url: String?) {
        locker.withWriteLock {
            if (url == null) {
                preferences.remove(key)
            } else {
                preferences.put(key, url)
            }
            preferences.flush()
        }
    }
}
