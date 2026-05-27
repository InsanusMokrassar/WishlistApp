package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.Constants
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
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

class KtorUsersManagementFeature(
    private val client: HttpClient
) : UsersManagementFeature {
    private val basePath = "${Constants.adminPrefixPathPart}/${Constants.usersPathPart}"

    override suspend fun getAll(): List<RegisteredUser> =
        client.get("$basePath/${Constants.usersGetAllPathPart}").body()

    override suspend fun getById(id: UserId): RegisteredUser? {
        val response = client.get("$basePath/${Constants.usersGetByIdPathPart}/${id.long}")
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun create(newUser: NewUserWithPassword): RegisteredUser? {
        val response = client.post("$basePath/${Constants.usersCreatePathPart}") {
            contentType(ContentType.Application.Json)
            setBody(newUser)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun update(id: UserId, newUser: NewUser): Boolean {
        val response = client.put("$basePath/${Constants.usersUpdatePathPart}/${id.long}") {
            contentType(ContentType.Application.Json)
            setBody(newUser)
        }
        return response.status.isSuccess()
    }

    override suspend fun delete(id: UserId): Boolean {
        val response = client.delete("$basePath/${Constants.usersDeletePathPart}/${id.long}")
        return response.status.isSuccess()
    }
}
