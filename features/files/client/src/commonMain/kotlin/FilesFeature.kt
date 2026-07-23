package dev.inmo.wishlist.features.files.client

import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Client-facing contract for the files feature: pure HTTP operations against the server routes.
 *
 * Higher-level concerns (performing the temporal upload, deriving MIME type, building download URLs)
 * live in [FilesClientService], following the codebase rule that `KtorXxxFeature` implementations
 * only call endpoints.
 */
interface FilesFeature {
    /**
     * Finalizes a previously uploaded temporal file into permanent storage.
     *
     * @param request Reference to the temporal upload plus metadata to persist.
     * @return Persisted [FilesFeatureMetaInfo], or `null` on a non-2xx response.
     */
    suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo?

    /**
     * Fetches metadata for a stored file.
     *
     * @param id Identifier of the file.
     * @return [FilesFeatureMetaInfo], or `null` when unknown / non-2xx response.
     */
    suspend fun getMeta(id: FileId): FilesFeatureMetaInfo?

    /**
     * Fetches the avatar file id currently set for [userId].
     *
     * @param userId Identity whose avatar to resolve.
     * @return Avatar [FileId], or `null` when the user has no avatar / non-2xx response.
     */
    suspend fun getAvatar(userId: UserId): FileId?

    /**
     * Associates the already-finalized file [fileId] as the avatar of [userId]. The server allows
     * this only for the user themselves or the `root` user.
     *
     * @param userId Identity whose avatar to set.
     * @param fileId Finalized file to use as the avatar.
     * @return `true` on a 2xx response; `false` otherwise (forbidden, unknown file, network error).
     */
    suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean
}
