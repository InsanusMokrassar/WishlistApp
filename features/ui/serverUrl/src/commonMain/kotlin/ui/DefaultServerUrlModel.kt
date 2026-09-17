package dev.inmo.wishlist.features.ui.serverUrl.ui

import dev.inmo.wishlist.features.auth.client.ServerUrlStorage

/**
 * Default server URL model backed by the shared persistent URL storage.

 * @param storage Persistent server URL storage.
 */
class DefaultServerUrlModel(
    private val storage: ServerUrlStorage,
) : ServerUrlModel {
    /** @return The persisted server URL, or `null` when unset. */
    override suspend fun getServerUrl(): String? = storage.getServerUrl()

    /** Saves [url], converting blank input to `null` without trimming nonblank input. */
    override suspend fun saveServerUrl(url: String?) {
        storage.saveServerUrl(url?.takeIf { it.isNotBlank() })
    }
}
