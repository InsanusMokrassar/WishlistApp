package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [FilesFeatureMetaInfo]'s wire shape and its [asFilesFeatureMetaInfo] mapper. */
class FilesFeatureMetaInfoTest {

    /** Encoded JSON carries exactly the metadata's five declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val meta = FilesFeatureMetaInfo(
            id = FileId("file-1"),
            fileName = FileName("avatar.png"),
            mimeType = "image/png",
            size = 1024L,
            uploaderId = UserId(1L)
        )

        val json = Json.encodeToJsonElement(FilesFeatureMetaInfo.serializer(), meta).jsonObject

        assertEquals(setOf("id", "fileName", "mimeType", "size", "uploaderId"), json.keys)
    }

    /** Every field is projected unchanged from the source [RegisteredFileMetaInfo]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredFileMetaInfo(
            id = FileId("file-1"),
            fileName = FileName("avatar.png"),
            mimeType = "image/png",
            size = 1024L,
            uploaderId = UserId(1L)
        )

        assertEquals(
            FilesFeatureMetaInfo(
                id = FileId("file-1"),
                fileName = FileName("avatar.png"),
                mimeType = "image/png",
                size = 1024L,
                uploaderId = UserId(1L)
            ),
            registered.asFilesFeatureMetaInfo()
        )
    }
}
