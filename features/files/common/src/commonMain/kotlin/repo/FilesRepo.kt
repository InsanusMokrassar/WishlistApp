package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.wishlist.features.files.common.models.FileId

/**
 * Binary store for file payloads, keyed by [FileId].
 *
 * Declared in `commonMain` so the [FileId]-keyed contract is shared, but only the server has a
 * concrete implementation (disk-backed) — clients never read bytes through this interface, they
 * fetch them over HTTP via the download route.
 */
interface FilesRepo {
    /**
     * Persists [bytes] under [id], overwriting any existing payload for that id.
     *
     * @param id Identifier to store the payload under.
     * @param bytes Raw file content.
     */
    suspend fun put(id: FileId, bytes: ByteArray)

    /**
     * Reads the payload stored under [id].
     *
     * @param id Identifier to read.
     * @return Raw bytes, or `null` when no payload exists for [id].
     */
    suspend fun get(id: FileId): ByteArray?

    /**
     * Removes the payload stored under [id]; no-op when absent.
     *
     * @param id Identifier to remove.
     */
    suspend fun remove(id: FileId)
}
