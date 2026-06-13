package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.models.CopyWishlistRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

/**
 * Ktor HTTP client implementation of [WishlistCopyFeature].
 *
 * Posts the copy request to `/wishlist/copy`; the server resolves the recipient from the bearer
 * token attached by the `HttpClientConfigurator` chain and enqueues a background copy job.
 *
 * @param client Preconfigured Ktor [HttpClient] injected from Koin.
 */
class KtorWishlistCopyFeature(
    private val client: HttpClient
) : WishlistCopyFeature {
    /**
     * Posts [request] as JSON and reports whether the server accepted the job (`202 Accepted`).
     *
     * @param request Source wishlist to copy.
     * @return `true` on 2xx response.
     */
    override suspend fun enqueueCopy(request: CopyWishlistRequest): Boolean {
        val response = client.post("${Constants.wishlistPrefixPathPart}/${Constants.wishlistCopyPathPart}") {
            setBody(request)
        }
        return response.status.isSuccess()
    }
}
