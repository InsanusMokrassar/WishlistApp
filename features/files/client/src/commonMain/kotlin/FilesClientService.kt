package dev.inmo.wishlist.features.files.client

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.common.filename
import dev.inmo.micro_utils.ktor.client.tempUpload
import dev.inmo.wishlist.features.files.client.utils.imageMimeType
import dev.inmo.wishlist.features.files.common.Constants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo
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
        val temporalFileId = client.tempUpload(Constants.temporalUploadPathPart, file)
        return feature.finalize(
            FinalizeFileRequest(
                temporalFileId = temporalFileId,
                fileName = file.filename,
                mimeType = file.filename.imageMimeType()
            )
        )
    }

    /**
     * Builds the relative download URL for [id] (resolved against the configured server base URL).
     *
     * @param id File identifier.
     * @return Path such as `files/<id>` usable as an image `src` or HTTP GET target.
     */
    fun fileUrl(id: FileId): String = "${Constants.filesPrefixPathPart}/${id.string}"

    /**
     * Downloads the raw bytes of the file [id] through the shared client. Used by platforms that
     * cannot render an image straight from a URL and must decode bytes themselves (JVM/Android).
     *
     * @param id File identifier.
     * @return Payload bytes, or `null` on a non-2xx response.
     */
    suspend fun downloadBytes(id: FileId): ByteArray? {
        val response = client.get(fileUrl(id))
        return if (response.status.isSuccess()) response.body() else null
    }
}
