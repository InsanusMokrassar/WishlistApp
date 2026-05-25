package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.WishlistsItemsFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Ktor HTTP client implementation of [WishlistsItemsFeature].
 *
 * Builds requests against the paths defined in [Constants] and delegates response
 * parsing to the configured content-negotiation serializer (JSON).
 * The underlying [client] is expected to have bearer auth and base-URL configured
 * by the `HttpClientConfigurator` chain registered in the common client plugin.
 *
 * Mutation requests ([create], [update], [delete]) carry no caller identifier — the server
 * enforces parent-wishlist ownership using the bearer token.
 *
 * @param client Preconfigured Ktor [HttpClient] injected from Koin.
 */
class KtorWishlistItemFeature(
    private val client: HttpClient
) : WishlistsItemsFeature {
    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        client.get("${Constants.wishlistItemPrefixPathPart}/${Constants.wishlistItemGetByWishlistIdPathPart}/${wishlistId.long}").body()

    /**
     * Posts [newWishlistItem] as JSON and deserialises the response body on success.
     *
     * Returns `null` on non-2xx response (includes 403 Forbidden when caller does not own the parent wishlist).
     *
     * @param newWishlistItem Item data to create.
     * @return Created [RegisteredWishlistItem], or `null` on non-2xx response.
     */
    override suspend fun create(newWishlistItem: NewWishlistItem): RegisteredWishlistItem? {
        val response = client.post("${Constants.wishlistItemPrefixPathPart}/${Constants.wishlistItemCreatePathPart}") {
            contentType(ContentType.Application.Json)
            setBody(newWishlistItem)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    /**
     * Sends [newWishlistItem] as JSON PUT and returns whether the server acknowledged success.
     *
     * Returns `false` on `403 Forbidden` (caller does not own the parent wishlist) or `404 Not Found`.
     *
     * @param id Identifier of the item to replace.
     * @param newWishlistItem Replacement data.
     * @return `true` on 2xx response.
     */
    override suspend fun update(id: WishlistItemId, newWishlistItem: NewWishlistItem): Boolean {
        val response = client.put("${Constants.wishlistItemPrefixPathPart}/${Constants.wishlistItemUpdatePathPart}/${id.long}") {
            contentType(ContentType.Application.Json)
            setBody(newWishlistItem)
        }
        return response.status.isSuccess()
    }

    /**
     * Sends a DELETE request and returns whether the server acknowledged success.
     *
     * Returns `false` on `403 Forbidden` (caller does not own the parent wishlist) or `404 Not Found`.
     *
     * @param id Identifier of the item to remove.
     * @return `true` on 2xx response.
     */
    override suspend fun delete(id: WishlistItemId): Boolean {
        val response = client.delete("${Constants.wishlistItemPrefixPathPart}/${Constants.wishlistItemDeletePathPart}/${id.long}")
        return response.status.isSuccess()
    }
}
