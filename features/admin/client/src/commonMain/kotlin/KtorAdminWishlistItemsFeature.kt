package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.Constants
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
import io.ktor.http.isSuccess

class KtorAdminWishlistItemsFeature(
    private val client: HttpClient
) : AdminWishlistItemsFeature {
    private val basePath = "${Constants.adminPrefixPathPart}/${Constants.wishlistItemsPathPart}"

    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        client.get("$basePath/${Constants.wishlistItemsGetByWishlistIdPathPart}/${wishlistId.long}").body()

    override suspend fun create(item: NewWishlistItem): RegisteredWishlistItem? {
        val response = client.post("$basePath/${Constants.wishlistItemsCreatePathPart}") {
            setBody(item)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun update(id: WishlistItemId, item: NewWishlistItem): Boolean {
        val response = client.put("$basePath/${Constants.wishlistItemsUpdatePathPart}/${id.long}") {
            setBody(item)
        }
        return response.status.isSuccess()
    }

    override suspend fun delete(id: WishlistItemId): Boolean {
        val response = client.delete("$basePath/${Constants.wishlistItemsDeletePathPart}/${id.long}")
        return response.status.isSuccess()
    }
}
