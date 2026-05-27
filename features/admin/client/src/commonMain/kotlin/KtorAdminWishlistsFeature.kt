package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.Constants
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
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
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class KtorAdminWishlistsFeature(
    private val client: HttpClient
) : AdminWishlistsFeature {
    private val basePath = "${Constants.adminPrefixPathPart}/${Constants.wishlistsPathPart}"

    override suspend fun getAll(): List<RegisteredWishlist> =
        client.get("$basePath/${Constants.wishlistsGetAllPathPart}").body()

    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        client.get("$basePath/${Constants.wishlistsGetByUserIdPathPart}/${userId.long}").body()

    override suspend fun getById(id: WishlistId): RegisteredWishlist? {
        val response = client.get("$basePath/${Constants.wishlistsGetByIdPathPart}/${id.long}")
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun create(newWishlist: NewWishlist): RegisteredWishlist? {
        val response = client.post("$basePath/${Constants.wishlistsCreatePathPart}") {
            contentType(ContentType.Application.Json)
            setBody(newWishlist)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean {
        val response = client.put("$basePath/${Constants.wishlistsUpdatePathPart}/${id.long}") {
            contentType(ContentType.Application.Json)
            setBody(newWishlist)
        }
        return response.status.isSuccess()
    }

    override suspend fun delete(id: WishlistId): Boolean {
        val response = client.delete("$basePath/${Constants.wishlistsDeletePathPart}/${id.long}")
        return response.status.isSuccess()
    }
}
