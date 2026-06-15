package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the mapped [KeyValueRepo] backing [ExposedDeepLinksRepo].
 *
 * Stores each [StoredDeepLink] as a JSON string in the `deeplinks` table (`deeplink_id` text primary
 * key, `data_json` text), reusing the `ExposedKeyValueRepo` + `withMapper` pattern from
 * `ExposedFilesMetaInfoRepo`. JSON encoding keeps the embedded handler-info schema-flexible without
 * per-field columns.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to encode/decode the stored value.
 */
private fun createDelegate(database: Database, json: Json): KeyValueRepo<DeepLinkId, StoredDeepLink> =
    ExposedKeyValueRepo<String, String>(
        database = database,
        keyColumnAllocator = { text("deeplink_id") },
        valueColumnAllocator = { text("data_json") },
        tableName = "deeplinks"
    ).withMapper<DeepLinkId, StoredDeepLink, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(StoredDeepLink.serializer(), this) },
        keyToToFrom = { DeepLinkId(this) },
        valueToToFrom = { json.decodeFromString(StoredDeepLink.serializer(), this) }
    )

/**
 * Exposed JDBC implementation of [DeepLinksRepo] backed by the `deeplinks` table.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Serializer used to persist [StoredDeepLink] as JSON text.
 */
class ExposedDeepLinksRepo(
    database: Database,
    json: Json
) : DeepLinksRepo, KeyValueRepo<DeepLinkId, StoredDeepLink> by createDelegate(database, json)
