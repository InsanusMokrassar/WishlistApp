package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.common.FileName
import dev.inmo.micro_utils.ktor.common.TemporalFileId
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.client.FilesFeature
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Covers JVM-only file arguments used by the default wishlist model. */
class WishlistsModelFileTest {
    @Test
    fun uploadAndDownloadDelegateThroughFilesService() = runTest {
        val requestedPaths = mutableListOf<String>()
        val payload = "downloaded-image".encodeToByteArray()
        val client = HttpClient(MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when (request.method) {
                HttpMethod.Post -> respond("temporal-id", HttpStatusCode.OK)
                HttpMethod.Get -> respond(payload.decodeToString(), HttpStatusCode.OK)
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        })
        val feature = RecordingFilesFeature()
        val model = WishlistsModelTest.modelForFileTests(FilesClientService(client, feature), backgroundScope)
        val file = File.createTempFile("wishlist-model-", ".png").apply {
            writeBytes(byteArrayOf(4, 5, 6))
        }
        try {
            assertEquals(feature.fileId, model.uploadImage(file))
            assertEquals(
                listOf(
                    FinalizeFileRequest(
                        temporalFileId = TemporalFileId("temporal-id"),
                        fileName = FileName(file.name),
                        mimeType = "image/png",
                    )
                ),
                feature.finalizeCalls,
            )

            assertContentEquals(payload, model.loadImageBytes(feature.fileId))
            assertEquals(listOf("/temp_upload", "/files/${feature.fileId.string}"), requestedPaths)
        } finally {
            file.delete()
            client.close()
        }
    }

    private class RecordingFilesFeature : FilesFeature {
        val fileId = FileId("stored-image")
        val finalizeCalls = mutableListOf<FinalizeFileRequest>()

        override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo {
            finalizeCalls += request
            return FilesFeatureMetaInfo(
                id = fileId,
                fileName = request.fileName,
                mimeType = request.mimeType,
                size = 3L,
                uploaderId = UserId(5L),
            )
        }

        override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? = null
        override suspend fun getAvatar(userId: UserId): FileId? = null
        override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean = false
    }
}
