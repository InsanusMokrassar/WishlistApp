package dev.inmo.wishlist.features.files.client

import dev.inmo.wishlist.features.files.common.Constants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

/**
 * Ktor HTTP client implementation of [FilesFeature].
 *
 * Issues requests against the paths in [Constants] and relies on the shared [client]'s
 * `HttpClientConfigurator` chain for base URL and bearer auth. Performs no additional logic —
 * temporal upload and URL building are handled by [FilesClientService].
 *
 * @param client Preconfigured Ktor [HttpClient] injected from Koin.
 */
class KtorFilesFeature(
    private val client: HttpClient
) : FilesFeature {
    override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo? {
        val response = client.post("${Constants.filesPrefixPathPart}/${Constants.finalizePathPart}") {
            setBody(request)
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? {
        val response = client.get("${Constants.filesPrefixPathPart}/${Constants.metaPathPart}/${id.string}")
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun getAvatar(userId: UserId): FileId? {
        val response = client.get("${Constants.filesPrefixPathPart}/${Constants.avatarPathPart}/${userId.long}")
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean {
        val response = client.put("${Constants.filesPrefixPathPart}/${Constants.avatarPathPart}/${userId.long}") {
            setBody(fileId)
        }
        return response.status.isSuccess()
    }
}
