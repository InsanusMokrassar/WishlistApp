package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.files.client.FilesFeature.finalize]/`getMeta`
 * (and the server-side [dev.inmo.wishlist.features.files.server.services.FilesService] equivalents).
 *
 * Mirrors [RegisteredFileMetaInfo] verbatim; introduced so the files feature returns its own model
 * instead of the persistence entity directly, per the Feature Interface Return Model Rule.
 *
 * @property id Identifier under which the payload is stored.
 * @property fileName Original file name supplied by the client.
 * @property mimeType MIME type of the payload.
 * @property size Size of the payload in bytes.
 * @property uploaderId Uploading user.
 */
@Serializable
data class FilesFeatureMetaInfo(
    val id: FileId,
    val fileName: FileName,
    val mimeType: String,
    val size: Long,
    val uploaderId: UserId
)

/**
 * Projects this [RegisteredFileMetaInfo] onto [FilesFeatureMetaInfo], carrying every field through
 * unchanged.
 *
 * @return A [FilesFeatureMetaInfo] mirroring this file's full metadata field set.
 */
fun RegisteredFileMetaInfo.asFilesFeatureMetaInfo(): FilesFeatureMetaInfo = FilesFeatureMetaInfo(
    id = id,
    fileName = fileName,
    mimeType = mimeType,
    size = size,
    uploaderId = uploaderId
)
