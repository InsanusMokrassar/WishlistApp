package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import java.util.prefs.Preferences

class PreferencesAuthCredentialsStorage(
    private val json: Json,
    nodeName: String = "wishlist/auth",
    private val key: String = "credentials"
) : AuthCredentialsStorage {
    private val preferences: Preferences = Preferences.userRoot().node(nodeName)
    private val locker = SmartRWLocker()
    private val _userAuthorised = MutableRedeliverStateFlow(preferences.get(key, null) != null)
    override val userAuthorised: StateFlow<Boolean> = _userAuthorised.asStateFlow()

    override suspend fun get(): AuthCredentials? = locker.withReadAcquire {
        preferences.get(key, null)?.let { json.decodeFromString(AuthCredentials.serializer(), it) }
    }

    override suspend fun save(credentials: AuthCredentials?) {
        locker.withWriteLock {
            if (credentials == null) {
                preferences.remove(key)
                _userAuthorised.value = false
            } else {
                preferences.put(key, json.encodeToString(AuthCredentials.serializer(), credentials))
                _userAuthorised.value = true
            }
            preferences.flush()
        }
    }
}
