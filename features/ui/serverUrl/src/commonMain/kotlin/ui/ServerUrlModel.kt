package dev.inmo.wishlist.features.ui.serverUrl.ui

/**
 * Model facade consumed by [ServerUrlViewModel].
 *
 * Backed by `features/auth/client`'s `ServerUrlStorage`.
 */
interface ServerUrlModel {
    /**
     * Returns the persisted server URL, or `null` when none saved yet.
     */
    suspend fun getServerUrl(): String?

    /**
     * Persists [url] as the active server URL.
     *
     * @param url URL to save; pass `null` or blank to clear (callers normally
     * guard against blank input).
     */
    suspend fun saveServerUrl(url: String?)
}
