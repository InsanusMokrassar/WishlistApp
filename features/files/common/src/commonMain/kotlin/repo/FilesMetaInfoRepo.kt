package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo

/**
 * Metadata store for files, keyed by [FileId]. Holds the descriptive
 * [RegisteredFileMetaInfo] (name, MIME type, size, owner) separately from the binary payload in
 * [FilesRepo], so the server can authorize and label a file without loading its bytes.
 *
 * Implemented on the server by an Exposed-backed [KeyValueRepo] (see `ExposedFilesMetaInfoRepo`).
 */
interface FilesMetaInfoRepo : KeyValueRepo<FileId, RegisteredFileMetaInfo>
