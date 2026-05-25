package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set
import project_group.project_name.features.auth.common.models.AuthCredentials

class LocalStorageAuthCredentialsStorage(
    private val json: Json,
    private val key: String = "project_name.auth.credentials"
) : AuthCredentialsStorage {
    private val _userAuthorised = MutableRedeliverStateFlow(localStorage[key] != null)
    override val userAuthorised: StateFlow<Boolean> = _userAuthorised.asStateFlow()
    private val locker = SmartRWLocker()

    override suspend fun get(): AuthCredentials? = locker.withReadAcquire {
        localStorage[key]?.let { json.decodeFromString(AuthCredentials.serializer(), it) }
    }

    override suspend fun save(credentials: AuthCredentials?) {
        locker.withWriteLock {
            if (credentials == null) {
                localStorage.removeItem(key)
                _userAuthorised.value = false
            } else {
                localStorage[key] = json.encodeToString(AuthCredentials.serializer(), credentials)
                _userAuthorised.value = true
            }
        }
    }
}
