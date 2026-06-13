package dev.inmo.wishlist.features.auth.client

import android.content.Context
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials

class SharedPreferencesAuthCredentialsStorage(
    context: Context,
    private val json: Json,
    fileName: String = "wishlist.auth",
    private val key: String = "credentials"
) : AuthCredentialsStorage {
    private val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val locker = SmartRWLocker()
    private val _userAuthorised = MutableRedeliverStateFlow(prefs.getString(key, null) != null)
    override val userAuthorised: StateFlow<Boolean> = _userAuthorised.asStateFlow()

    override suspend fun get(): AuthCredentials? = locker.withReadAcquire {
        prefs.getString(key, null)?.let { json.decodeFromString(AuthCredentials.serializer(), it) }
    }

    override suspend fun save(credentials: AuthCredentials?) {
        locker.withWriteLock {
            val editor = prefs.edit()
            if (credentials == null) {
                editor.remove(key)
                _userAuthorised.value = false
            } else {
                editor.putString(key, json.encodeToString(AuthCredentials.serializer(), credentials))
                _userAuthorised.value = true
            }
            editor.apply()
        }
    }
}
