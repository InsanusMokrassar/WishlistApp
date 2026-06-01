package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the mapped [KeyValueRepo] backing [ExposedFilesMetaInfoRepo].
 *
 * Stores each [RegisteredFileMetaInfo] as a JSON string in the `files_meta` table (`file_id` text
 * primary key, `meta_json` text), reusing the `ExposedKeyValueRepo` + `withMapper` pattern from
 * `ExposedPasswordsRepo`. JSON encoding keeps the metadata schema-flexible without per-field columns.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to encode/decode the stored value.
 */
private fun createDelegate(database: Database, json: Json): KeyValueRepo<FileId, RegisteredFileMetaInfo> =
    ExposedKeyValueRepo<String, String>(
        database = database,
        keyColumnAllocator = { text("file_id") },
        valueColumnAllocator = { text("meta_json") },
        tableName = "files_meta"
    ).withMapper<FileId, RegisteredFileMetaInfo, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(RegisteredFileMetaInfo.serializer(), this) },
        keyToToFrom = { FileId(this) },
        valueToToFrom = { json.decodeFromString(RegisteredFileMetaInfo.serializer(), this) }
    )

/**
 * Exposed JDBC implementation of [FilesMetaInfoRepo] backed by the `files_meta` table.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to persist [RegisteredFileMetaInfo] as JSON text.
 */
class ExposedFilesMetaInfoRepo(
    database: Database,
    json: Json
) : FilesMetaInfoRepo, KeyValueRepo<FileId, RegisteredFileMetaInfo> by createDelegate(database, json)
