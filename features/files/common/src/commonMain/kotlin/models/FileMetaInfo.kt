package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Descriptive metadata about a stored file. The binary payload itself lives in
 * [dev.inmo.wishlist.features.files.common.repo.FilesRepo]; this type captures everything needed
 * to serve, label and authorize that payload.
 *
 * Sealed to allow exhaustive handling of the not-yet-persisted [NewFileMetaInfo] and the
 * persisted [RegisteredFileMetaInfo] variants (mirrors the New/Registered split used across the
 * codebase, e.g. `WishlistItem`, `User`).
 */
@Serializable
sealed interface FileMetaInfo {
    /** Original file name as supplied by the uploading client. */
    val fileName: FileName

    /** MIME type of the payload (e.g. `image/png`). For this app it is constrained to image types. */
    val mimeType: String

    /** Size of the payload in bytes. */
    val size: Long

    /** Identity of the user that uploaded the file. */
    val ownerId: UserId
}

/**
 * File metadata not yet persisted — used as input to the store operation.
 *
 * @property fileName Original file name supplied by the client.
 * @property mimeType MIME type of the payload.
 * @property size Size of the payload in bytes.
 * @property ownerId Uploading user.
 */
@Serializable
data class NewFileMetaInfo(
    override val fileName: FileName,
    override val mimeType: String,
    override val size: Long,
    override val ownerId: UserId
) : FileMetaInfo

/**
 * File metadata already persisted, carrying the assigned [id].
 *
 * @property id Identifier under which the payload is stored.
 * @property fileName Original file name supplied by the client.
 * @property mimeType MIME type of the payload.
 * @property size Size of the payload in bytes.
 * @property ownerId Uploading user.
 */
@Serializable
data class RegisteredFileMetaInfo(
    val id: FileId,
    override val fileName: FileName,
    override val mimeType: String,
    override val size: Long,
    override val ownerId: UserId
) : FileMetaInfo
