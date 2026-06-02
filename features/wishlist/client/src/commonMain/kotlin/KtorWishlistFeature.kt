package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

/**
 * Ktor HTTP client implementation of [WishlistsFeature].
 *
 * Mutation requests ([create], [update]) send [NewWishlistInFeature] — a payload that
 * carries no user identifier. The server resolves the caller identity from the bearer
 * token attached by the `HttpClientConfigurator` chain.
 *
 * The underlying [client] is expected to have bearer auth and base-URL configured
 * by the `HttpClientConfigurator` chain registered in the common client plugin.
 *
 * @param client Preconfigured Ktor [HttpClient] injected from Koin.
 */
class KtorWishlistFeature(
    private val client: HttpClient
) : WishlistsFeature {
    /**
     * Fetches a single wishlist by [id] from the public endpoint (no auth token required).
     *
     * @param id Wishlist primary key.
     * @return [RegisteredWishlist] on 2xx, `null` on 404 or any non-2xx response.
     */
    override suspend fun getById(id: WishlistId): RegisteredWishlist? {
        val response = client.get("${Constants.wishlistPrefixPathPart}/${Constants.wishlistGetByIdPathPart}/${id.long}")
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        client.get("${Constants.wishlistPrefixPathPart}/${Constants.wishlistGetByUserIdPathPart}/${userId.long}").body()

    /**
     * Calls the `getMy` endpoint; the server resolves the caller from the bearer token.
     *
     * @return Wishlists owned by the authenticated caller.
     */
    override suspend fun getMyWishlists(): List<RegisteredWishlist> =
        client.get("${Constants.wishlistPrefixPathPart}/${Constants.wishlistGetMyPathPart}").body()

    /**
     * Posts [newWishlist] as JSON (no user identifier) and deserialises the response on success.
     *
     * @param newWishlist Wishlist data to create; user resolved server-side from auth token.
     * @return Created [RegisteredWishlist], or `null` on non-2xx response.
     */
    override suspend fun create(newWishlist: NewWishlistInFeature): RegisteredWishlist? {
        val response = client.post("${Constants.wishlistPrefixPathPart}/${Constants.wishlistCreatePathPart}") {
            setBody(newWishlist)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    /**
     * Sends [newWishlist] as JSON PUT and returns whether the server acknowledged success.
     *
     * Returns `false` on `403 Forbidden` (caller is not the owner) or `404 Not Found`.
     *
     * @param id Identifier of the wishlist to replace.
     * @param newWishlist Replacement data; user ownership enforced server-side.
     * @return `true` on 2xx response.
     */
    override suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean {
        val response = client.put("${Constants.wishlistPrefixPathPart}/${Constants.wishlistUpdatePathPart}/${id.long}") {
            setBody(newWishlist)
        }
        return response.status.isSuccess()
    }

    /**
     * Sends a DELETE request and returns whether the server acknowledged success.
     *
     * Returns `false` on `403 Forbidden` (caller is not the owner) or `404 Not Found`.
     *
     * @param id Identifier of the wishlist to remove.
     * @return `true` on 2xx response.
     */
    override suspend fun delete(id: WishlistId): Boolean {
        val response = client.delete("${Constants.wishlistPrefixPathPart}/${Constants.wishlistDeletePathPart}/${id.long}")
        return response.status.isSuccess()
    }
}
