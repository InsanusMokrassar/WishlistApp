package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.micro_utils.ktor.common.TemporalFileId
import kotlinx.serialization.Serializable

/**
 * JSON body of the `POST /files/finalize` request. Sent by the client after a successful temporal
 * upload to promote a temporary file into permanent storage.
 *
 * The two-step flow is: (1) the client uploads raw bytes to the shared temporal endpoint
 * (`POST /temp_upload`, served by MicroUtils `TemporalFilesRoutingConfigurator`) and receives a
 * [TemporalFileId]; (2) the client posts this request referencing that id plus the descriptive
 * metadata it knows locally. The server moves the temp file into [dev.inmo.wishlist.features.files.common.repo.FilesRepo]
 * and records [dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo].
 *
 * @property temporalFileId Id of the previously uploaded temporary file.
 * @property fileName Original file name to persist in the metadata.
 * @property mimeType MIME type to persist; validated server-side to be an image type.
 */
@Serializable
data class FinalizeFileRequest(
    val temporalFileId: TemporalFileId,
    val fileName: FileName,
    val mimeType: String
)
