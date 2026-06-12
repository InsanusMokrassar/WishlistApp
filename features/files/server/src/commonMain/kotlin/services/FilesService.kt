package dev.inmo.wishlist.features.files.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.ktor.server.TemporalFilesRoutingConfigurator
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo
import dev.inmo.wishlist.features.files.common.repo.FilesMetaInfoRepo
import dev.inmo.wishlist.features.files.common.repo.FilesRepo
import dev.inmo.wishlist.features.files.common.repo.UserAvatarsRepo
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side files service. Promotes temporarily-uploaded files into permanent storage, records
 * their metadata, and serves bytes/metadata back to routes.
 *
 * The temporal upload itself is handled by the shared MicroUtils [TemporalFilesRoutingConfigurator]
 * (`POST /temp_upload`); this service consumes the resulting temp file by id during [finalize].
 *
 * This is a server-only service (not bound to a client-facing interface) because [finalize] carries
 * an explicit caller [UserId] absent from the client contract — same convention as `WishlistItemService`.
 *
 * @param temporalFiles Shared temporal-upload configurator holding pending temp files by id.
 * @param filesRepo Binary store for permanent payloads.
 * @param metaInfoRepo Metadata store keyed by [FileId].
 * @param userAvatarsRepo Association store mapping a user to that user's avatar [FileId].
 */
class FilesService(
    private val temporalFiles: TemporalFilesRoutingConfigurator,
    private val filesRepo: FilesRepo,
    private val metaInfoRepo: FilesMetaInfoRepo,
    private val userAvatarsRepo: UserAvatarsRepo
) {
    /**
     * Moves the temp file referenced by [request] into permanent storage and records its metadata.
     *
     * Rejects (returns `null`) when the temp file is missing/expired or when the payload is not an
     * allowlisted raster image whose bytes match the declared type (see [isSupportedRasterImage]).
     * The temp file is always consumed (read then deleted) before validation so a rejected upload
     * leaves nothing behind.
     *
     * @param request Reference to the temporal upload plus the metadata to persist.
     * @param callerId Authenticated uploader, recorded as the file owner.
     * @return Persisted [RegisteredFileMetaInfo], or `null` on missing temp file or rejected payload.
     */
    suspend fun finalize(request: FinalizeFileRequest, callerId: UserId): RegisteredFileMetaInfo? {
        val tempFile = temporalFiles.getAndRemoveTemporalFile(request.temporalFileId) ?: return null
        val bytes = tempFile.readBytes()
        tempFile.delete()
        if (!isSupportedRasterImage(request.mimeType, bytes)) return null
        val fileId = FileId(uuid4().toString())
        filesRepo.put(fileId, bytes)
        val meta = RegisteredFileMetaInfo(
            id = fileId,
            fileName = request.fileName,
            mimeType = request.mimeType,
            size = bytes.size.toLong(),
            uploaderId = callerId
        )
        metaInfoRepo.set(fileId, meta)
        return meta
    }

    /**
     * Returns the avatar [FileId] currently associated with [userId], or `null` when the user has
     * no avatar set.
     *
     * @param userId Identity whose avatar is requested.
     */
    suspend fun getAvatar(userId: UserId): FileId? = userAvatarsRepo.get(userId)

    /**
     * Associates the already-finalized file [fileId] as the avatar of [userId], overwriting any
     * previous association. Authorization (caller must be the user or root) is enforced by the route.
     *
     * @param userId Identity whose avatar is being set.
     * @param fileId Finalized file to use as the avatar.
     * @return `true` when [fileId] references an existing finalized file and the association was
     * stored; `false` when no such file exists.
     */
    suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean {
        if (metaInfoRepo.get(fileId) == null) return false
        userAvatarsRepo.set(userId, fileId)
        return true
    }

    /**
     * Returns the metadata stored for [id], or `null` when unknown.
     *
     * @param id File identifier.
     */
    suspend fun getMeta(id: FileId): RegisteredFileMetaInfo? = metaInfoRepo.get(id)

    /**
     * Returns the raw payload stored for [id], or `null` when absent.
     *
     * @param id File identifier.
     */
    suspend fun getBytes(id: FileId): ByteArray? = filesRepo.get(id)

    /**
     * Validates that a finalized payload is a supported raster image whose declared [mimeType]
     * matches the actual file signature.
     *
     * Only the fixed allowlist in [allowedImageSignatures] is accepted. Vector formats — notably
     * `image/svg+xml`, which can carry active script — and any payload whose leading bytes do not
     * match the declared type are rejected. This prevents a malicious file from being stored and
     * later served as active content from the application origin.
     *
     * @param mimeType Client-declared MIME type; parameters such as `; charset=…` are ignored.
     * @param bytes Full payload to inspect.
     * @return `true` only when [mimeType] is allowlisted and [bytes] start with the matching signature.
     */
    private fun isSupportedRasterImage(mimeType: String, bytes: ByteArray): Boolean {
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        val matchesSignature = allowedImageSignatures[normalized] ?: return false
        return matchesSignature(bytes)
    }

    companion object {
        /**
         * Allowlist of accepted image MIME types, each mapped to a predicate verifying the payload's
         * leading "magic" bytes. An entry here is necessary but not sufficient: the bytes must also
         * match, so a renamed non-image (or an SVG) cannot pass by merely claiming an allowed type.
         */
        private val allowedImageSignatures: Map<String, (ByteArray) -> Boolean> = mapOf(
            "image/png" to { bytes: ByteArray -> bytes.startsWithBytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) },
            "image/jpeg" to { bytes: ByteArray -> bytes.startsWithBytes(0xFF, 0xD8, 0xFF) },
            "image/gif" to { bytes: ByteArray -> bytes.startsWithBytes(0x47, 0x49, 0x46, 0x38) },
            "image/bmp" to { bytes: ByteArray -> bytes.startsWithBytes(0x42, 0x4D) },
            "image/webp" to { bytes: ByteArray ->
                bytes.startsWithBytes(0x52, 0x49, 0x46, 0x46) && bytes.bytesAtMatch(8, 0x57, 0x45, 0x42, 0x50)
            }
        )

        /**
         * Returns `true` when the receiver's first bytes equal [expected], compared as unsigned 0..255.
         *
         * @param expected Expected leading byte values.
         */
        private fun ByteArray.startsWithBytes(vararg expected: Int): Boolean = bytesAtMatch(0, *expected)

        /**
         * Returns `true` when the bytes starting at [offset] equal [expected], compared as unsigned 0..255.
         *
         * @param offset Index of the first byte to compare.
         * @param expected Expected byte values at [offset].
         */
        private fun ByteArray.bytesAtMatch(offset: Int, vararg expected: Int): Boolean {
            if (size < offset + expected.size) return false
            for (index in expected.indices) {
                if (this[offset + index].toInt() and 0xFF != expected[index]) return false
            }
            return true
        }
    }
}
