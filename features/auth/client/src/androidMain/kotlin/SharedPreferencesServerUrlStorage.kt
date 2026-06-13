package dev.inmo.wishlist.features.auth.client

import android.content.Context
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock

class SharedPreferencesServerUrlStorage(
    context: Context,
    fileName: String = "wishlist.serverAddress",
    private val key: String = "url"
) : ServerUrlStorage {
    private val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val locker = SmartRWLocker()

    override suspend fun getServerUrl(): String? = locker.withReadAcquire {
        prefs.getString(key, null)
    }

    override suspend fun saveServerUrl(url: String?) {
        locker.withWriteLock {
            val editor = prefs.edit()
            if (url == null) editor.remove(key) else editor.putString(key, url)
            editor.apply()
        }
    }
}
