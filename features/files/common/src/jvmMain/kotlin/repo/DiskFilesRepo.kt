package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.wishlist.features.files.common.models.FileId
import java.io.File

/**
 * Disk-backed [FilesRepo]. Each payload is stored as a single file named after its [FileId] inside
 * [folder]. Concurrent access is guarded by a [SmartRWLocker] so a reader serving a download cannot
 * observe a half-written file during a concurrent finalize.
 *
 * @param folder Directory under which payloads are stored; created on construction if absent.
 */
class DiskFilesRepo(
    private val folder: File
) : FilesRepo {
    private val locker = SmartRWLocker()

    init {
        folder.mkdirs()
    }

    /** Resolves the on-disk file for [id]. The [FileId] string is a server-generated UUID, so it is filesystem-safe. */
    private fun fileFor(id: FileId): File = File(folder, id.string)

    override suspend fun put(id: FileId, bytes: ByteArray) {
        locker.withWriteLock {
            fileFor(id).writeBytes(bytes)
        }
    }

    override suspend fun get(id: FileId): ByteArray? = locker.withReadAcquire {
        fileFor(id).takeIf { it.isFile }?.readBytes()
    }

    override suspend fun remove(id: FileId) {
        locker.withWriteLock {
            fileFor(id).delete()
        }
    }
}
