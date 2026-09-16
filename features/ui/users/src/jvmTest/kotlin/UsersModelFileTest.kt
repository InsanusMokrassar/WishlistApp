package dev.inmo.wishlist.features.ui.users

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

/** Covers the JVM-only file arguments used by the default users model. */
class UsersModelFileTest {
    @Test
    fun uploadAvatarAndDownloadBytesDelegateThroughFilesService() = runTest {
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
        val model = UsersModelTest.modelForFileTests(FilesClientService(client, feature), backgroundScope)
        val file = File.createTempFile("avatar-model-", ".png").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        try {
            val userId = UserId(44L)

            assertEquals(feature.fileId, model.uploadAvatar(userId, file))
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
            assertEquals(listOf(userId to feature.fileId), feature.setAvatarCalls)

            assertContentEquals(payload, model.loadImageBytes(feature.fileId))
            assertEquals(listOf("/temp_upload", "/files/${feature.fileId.string}"), requestedPaths)
        } finally {
            file.delete()
            client.close()
        }
    }

    private class RecordingFilesFeature : FilesFeature {
        val fileId = FileId("stored-avatar")
        val finalizeCalls = mutableListOf<FinalizeFileRequest>()
        val setAvatarCalls = mutableListOf<Pair<UserId, FileId>>()

        override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo {
            finalizeCalls += request
            return FilesFeatureMetaInfo(
                id = fileId,
                fileName = request.fileName,
                mimeType = request.mimeType,
                size = 3L,
                uploaderId = UserId(44L),
            )
        }

        override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean {
            setAvatarCalls += userId to fileId
            return true
        }

        override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? = null
        override suspend fun getAvatar(userId: UserId): FileId? = null
    }
}
