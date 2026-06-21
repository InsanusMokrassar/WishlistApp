package dev.inmo.wishlist.features.files.client

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.common.filename
import dev.inmo.micro_utils.ktor.client.tempUpload
import dev.inmo.wishlist.features.common.common.apiPathPart
import dev.inmo.wishlist.features.files.client.utils.imageMimeType
import dev.inmo.wishlist.features.files.common.Constants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

/**
 * High-level client entry point for uploading and referencing files. Composes the two-step upload:
 * (1) stream raw bytes to the shared temporal endpoint via MicroUtils [tempUpload] — whose JS actual
 * uses `XMLHttpRequest`+`FormData`, lifting the Ktor-JS large-file limitation — then (2) [finalize]
 * the resulting temporal id into permanent storage through [FilesFeature].
 *
 * @param client Shared Ktor [HttpClient] (base URL + bearer auth already configured).
 * @param feature HTTP feature used to finalize and read metadata.
 */
class FilesClientService(
    private val client: HttpClient,
    private val feature: FilesFeature
) {
    /**
     * Uploads [file] to temporal storage and finalizes it into a permanent file.
     *
     * @param file File chosen by the user on the current platform.
     * @return The persisted [RegisteredFileMetaInfo], or `null` if the server rejected the finalize
     * (e.g. non-image MIME or expired temporal upload).
     */
    suspend fun uploadFile(file: MPPFile): RegisteredFileMetaInfo? {
        val temporalFileId = client.tempUpload("/$apiPathPart/${Constants.temporalUploadPathPart}", file)
        return feature.finalize(
            FinalizeFileRequest(
                temporalFileId = temporalFileId,
                fileName = file.filename,
                mimeType = file.filename.imageMimeType()
            )
        )
    }

    /**
     * Absolute, `/api`-prefixed download URL for [id], suitable as a browser image `src`.
     *
     * Browser-loaded `<img>` requests do not pass through the shared [HttpClient] (so the base-URL
     * `/api` prefix added by [DefaultUrlHttpClientConfigurator] does not apply to them); the prefix is
     * therefore baked in here, and the
     * leading slash anchors the URL to the site origin regardless of the current SPA route.
     *
     * @param id File identifier.
     * @return Path such as `/api/files/<id>` usable directly as an image `src`.
     */
    fun apiFileUrl(id: FileId): String = "/$apiPathPart/${Constants.filesPrefixPathPart}/${id.string}"

    /**
     * Bare, relative download path for [id] (e.g. `files/<id>`), for use through the shared [HttpClient].
     *
     * Deliberately carries no `api` prefix: the configured server base URL already carries `/api` (appended
     * by `DefaultUrlHttpClientConfigurator`), so prefixing here too would double it to `/api/api/...`. For a browser `<img>`
     * src (which bypasses the shared client), use [apiFileUrl] instead.
     *
     * @param id File identifier.
     * @return Relative path such as `files/<id>`.
     */
    fun fileUrl(id: FileId): String = "${Constants.filesPrefixPathPart}/${id.string}"

    /**
     * Downloads the raw bytes of the file [id] through the shared client. Used by platforms that
     * cannot render an image straight from a URL and must decode bytes themselves (JVM/Android).
     *
     * The path is left bare (no `api` prefix): the configured server base URL already carries `/api` (via
     * `DefaultUrlHttpClientConfigurator`), so prefixing here too would yield a doubled `/api/api/...`.
     *
     * @param id File identifier.
     * @return Payload bytes, or `null` on a non-2xx response.
     */
    suspend fun downloadBytes(id: FileId): ByteArray? {
        val response = client.get(fileUrl(id))
        return if (response.status.isSuccess()) response.body() else null
    }

    /**
     * Returns the avatar [FileId] currently set for [userId], or `null` when none / on failure.
     *
     * @param userId Identity whose avatar to resolve.
     */
    suspend fun getAvatar(userId: UserId): FileId? = feature.getAvatar(userId)

    /**
     * Uploads [file] and sets it as the avatar of [userId] in a single step (temporal upload →
     * finalize → associate). Authorized server-side for the user themselves or `root`.
     *
     * @param userId Identity whose avatar to set.
     * @param file Image chosen by the user on the current platform.
     * @return The stored avatar [FileId], or `null` when the upload, finalize or association failed.
     */
    suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? {
        val fileId = uploadFile(file)?.id ?: return null
        return if (feature.setAvatar(userId, fileId)) fileId else null
    }
}
