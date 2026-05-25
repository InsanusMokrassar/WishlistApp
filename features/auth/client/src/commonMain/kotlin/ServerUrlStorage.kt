package dev.inmo.wishlist.features.auth.client

interface ServerUrlStorage {
    suspend fun getServerUrl(): String?
    suspend fun saveServerUrl(url: String?)
}
