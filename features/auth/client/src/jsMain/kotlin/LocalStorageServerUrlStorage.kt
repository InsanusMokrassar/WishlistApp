package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

class LocalStorageServerUrlStorage(
    private val key: String = "project_name.serverAddress.url"
) : ServerUrlStorage {
    private val locker = SmartRWLocker()

    override suspend fun getServerUrl(): String? = locker.withReadAcquire {
        localStorage[key]
    }

    override suspend fun saveServerUrl(url: String?) {
        locker.withWriteLock {
            if (url == null) {
                localStorage.removeItem(key)
            } else {
                localStorage[key] = url
            }
        }
    }
}
