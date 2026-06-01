package dev.inmo.wishlist.features.files.client

import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo

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
     * @return Persisted [RegisteredFileMetaInfo], or `null` on a non-2xx response.
     */
    suspend fun finalize(request: FinalizeFileRequest): RegisteredFileMetaInfo?

    /**
     * Fetches metadata for a stored file.
     *
     * @param id Identifier of the file.
     * @return [RegisteredFileMetaInfo], or `null` when unknown / non-2xx response.
     */
    suspend fun getMeta(id: FileId): RegisteredFileMetaInfo?
}
