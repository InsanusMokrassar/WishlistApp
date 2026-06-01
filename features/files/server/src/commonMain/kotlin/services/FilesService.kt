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
     * Rejects (returns `null`) when the temp file is missing/expired or when the declared MIME type
     * is not an image. A non-image temp file is deleted before returning.
     *
     * @param request Reference to the temporal upload plus the metadata to persist.
     * @param callerId Authenticated uploader, recorded as the file owner.
     * @return Persisted [RegisteredFileMetaInfo], or `null` on missing temp file or non-image MIME.
     */
    suspend fun finalize(request: FinalizeFileRequest, callerId: UserId): RegisteredFileMetaInfo? {
        val tempFile = temporalFiles.getAndRemoveTemporalFile(request.temporalFileId) ?: return null
        if (!request.mimeType.startsWith("image/")) {
            tempFile.delete()
            return null
        }
        val bytes = tempFile.readBytes()
        tempFile.delete()
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
}
