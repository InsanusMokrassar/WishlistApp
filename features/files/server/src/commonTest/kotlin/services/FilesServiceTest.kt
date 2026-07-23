package dev.inmo.wishlist.features.files.server.services

import dev.inmo.micro_utils.common.FileName
import dev.inmo.micro_utils.ktor.common.TemporalFileId
import dev.inmo.micro_utils.ktor.server.TemporalFilesRoutingConfigurator
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo
import dev.inmo.wishlist.features.files.common.models.asFilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.repo.FilesMetaInfoRepo
import dev.inmo.wishlist.features.files.common.repo.FilesRepo
import dev.inmo.wishlist.features.files.common.repo.UserAvatarsRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** In-memory [FilesRepo] test double backed by a plain map (the production contract carries no CRUD/KeyValueRepo base). */
internal class FakeFilesRepo : FilesRepo {
    private val map = mutableMapOf<FileId, ByteArray>()
    override suspend fun put(id: FileId, bytes: ByteArray) { map[id] = bytes }
    override suspend fun get(id: FileId): ByteArray? = map[id]
    override suspend fun remove(id: FileId) { map.remove(id) }
}

/** In-memory [FilesMetaInfoRepo] test double delegating entirely to [MapKeyValueRepo]. */
internal class FakeFilesMetaInfoRepo : FilesMetaInfoRepo, KeyValueRepo<FileId, RegisteredFileMetaInfo> by MapKeyValueRepo()

/** In-memory [UserAvatarsRepo] test double delegating entirely to [MapKeyValueRepo]. */
internal class FakeUserAvatarsRepo : UserAvatarsRepo, KeyValueRepo<UserId, FileId> by MapKeyValueRepo()

/**
 * Inserts [file] into [configurator]'s private temp-file map via reflection, standing in for a real
 * multipart upload through the shared MicroUtils `POST /temp_upload` route (which has no in-process
 * seeding API — [TemporalFilesRoutingConfigurator] only fills its map from an actual HTTP request).
 * Safe here because `features/files/server` is JVM-only (`mppJavaProject`), matching
 * [TemporalFilesRoutingConfigurator]'s own `jvmMain`-only placement.
 *
 * @param configurator Real, freshly-constructed configurator instance to seed.
 * @param id Temporal id the seeded [file] becomes retrievable under.
 * @param file Backing temp file (already containing the payload bytes).
 */
private fun seedTemporalFile(configurator: TemporalFilesRoutingConfigurator, id: TemporalFileId, file: File) {
    val field = TemporalFilesRoutingConfigurator::class.java.getDeclaredField("temporalFilesMap")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val map = field.get(configurator) as MutableMap<TemporalFileId, File>
    map[id] = file
}

/** Minimal valid PNG signature bytes, accepted by [FilesService]'s `isSupportedRasterImage` allowlist. */
private val pngSignatureBytes = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02, 0x03
)

/** Verifies [FilesService.finalize]/[FilesService.getMeta] both return [FilesFeatureMetaInfo]. */
class FilesServiceTest {

    private val uploaderId = UserId(1L)

    private fun buildService(
        temporalFiles: TemporalFilesRoutingConfigurator = TemporalFilesRoutingConfigurator(),
        metaInfoRepo: FakeFilesMetaInfoRepo = FakeFilesMetaInfoRepo()
    ) = FilesService(temporalFiles, FakeFilesRepo(), metaInfoRepo, FakeUserAvatarsRepo())

    /** A valid, allowlisted PNG upload is promoted to permanent storage and returned as [FilesFeatureMetaInfo]. */
    @Test
    fun finalizeReturnsFeatureMetaInfoForValidImage() = runTest {
        val temporalFiles = TemporalFilesRoutingConfigurator()
        val tempFile = File.createTempFile("upload-test", ".png").apply { writeBytes(pngSignatureBytes) }
        val temporalId = TemporalFileId("temp-1")
        seedTemporalFile(temporalFiles, temporalId, tempFile)
        val service = buildService(temporalFiles)

        val result = service.finalize(
            FinalizeFileRequest(temporalId, FileName("avatar.png"), "image/png"),
            uploaderId
        )

        checkNotNull(result)
        assertEquals(FileName("avatar.png"), result.fileName)
        assertEquals("image/png", result.mimeType)
        assertEquals(pngSignatureBytes.size.toLong(), result.size)
        assertEquals(uploaderId, result.uploaderId)
    }

    /** A [FinalizeFileRequest] referencing an unknown/expired temp file is rejected with `null`. */
    @Test
    fun finalizeReturnsNullWhenTempFileMissing() = runTest {
        val service = buildService()

        val result = service.finalize(
            FinalizeFileRequest(TemporalFileId("unknown"), FileName("avatar.png"), "image/png"),
            uploaderId
        )

        assertNull(result)
    }

    /** [FilesService.getMeta] returns the stored metadata as [FilesFeatureMetaInfo] for a known id. */
    @Test
    fun getMetaReturnsFeatureMetaInfoForKnownId() = runTest {
        val metaInfoRepo = FakeFilesMetaInfoRepo()
        val stored = RegisteredFileMetaInfo(FileId("file-1"), FileName("avatar.png"), "image/png", 11L, uploaderId)
        metaInfoRepo.set(stored.id, stored)
        val service = buildService(metaInfoRepo = metaInfoRepo)

        assertEquals(stored.asFilesFeatureMetaInfo(), service.getMeta(stored.id))
    }

    /** [FilesService.getMeta] returns `null` for an id with no stored metadata. */
    @Test
    fun getMetaReturnsNullForUnknownId() = runTest {
        val service = buildService()

        assertNull(service.getMeta(FileId("missing")))
    }
}
