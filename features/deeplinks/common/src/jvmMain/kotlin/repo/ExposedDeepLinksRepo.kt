package dev.inmo.wishlist.features.deeplinks.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the mapped [KeyValueRepo] backing [ExposedDeepLinksRepo].
 *
 * Stores each [DeepLinkHandlerInfo] as a JSON string in the `deeplinks` table (`deeplink_id` text
 * primary key, `handler_info_json` text), reusing the `ExposedKeyValueRepo` + `withMapper` pattern
 * from `ExposedFilesMetaInfoRepo`. JSON encoding keeps the handler info schema-flexible without
 * per-field columns.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to encode/decode the stored value.
 */
private fun createDelegate(database: Database, json: Json): KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> =
    ExposedKeyValueRepo<String, String>(
        database = database,
        keyColumnAllocator = { text("deeplink_id") },
        valueColumnAllocator = { text("handler_info_json") },
        tableName = "deeplinks"
    ).withMapper<DeepLinkId, DeepLinkHandlerInfo, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(DeepLinkHandlerInfo.serializer(), this) },
        keyToToFrom = { DeepLinkId(this) },
        valueToToFrom = { json.decodeFromString(DeepLinkHandlerInfo.serializer(), this) }
    )

/**
 * Exposed JDBC implementation of [DeepLinksRepo] backed by the `deeplinks` table.
 *
 * Persists [DeepLinkHandlerInfo] as JSON text, so the whole record is stored opaquely in one column.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to persist [DeepLinkHandlerInfo] as JSON text.
 */
class ExposedDeepLinksRepo(
    database: Database,
    json: Json
) : DeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by createDelegate(database, json)
