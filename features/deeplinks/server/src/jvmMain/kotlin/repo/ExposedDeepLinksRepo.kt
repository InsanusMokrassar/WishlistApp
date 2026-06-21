package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the [KeyValueRepo] delegate backed by table `deeplinks`
 * (columns: `deeplink_id` TEXT, `data_json` TEXT).
 *
 * Uses [ExposedKeyValueRepo] with [withMapper] to expose typed [KeyValueRepo]<[DeepLinkId], [StoredDeepLink]>.
 * [ExposedKeyValueRepo] extends [ExposedReadKeyValueRepo] which calls `initTable()` in its own `init {}`;
 * no additional `initTable()` call is required here.
 *
 * @param database Exposed [Database] from Koin (provided by `features/common/server`).
 * @param json Shared [Json] for encoding/decoding [StoredDeepLink] to/from a JSON string column.
 * @return Typed [KeyValueRepo]<[DeepLinkId], [StoredDeepLink]> backed by the `deeplinks` table.
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
 * JVM Exposed JDBC implementation of [DeepLinksRepo].
 *
 * Stores each [StoredDeepLink] as a JSON string in the `deeplinks` table. The schema is
 * flexible: new fields in [StoredDeepLink] or changes to the embedded
 * [kotlinx.serialization.json.JsonElement] require no migration.
 *
 * @param database Exposed [Database] injected from Koin.
 * @param json Shared [Json] injected from Koin.
 */
class ExposedDeepLinksRepo(
    database: Database,
    json: Json
) : DeepLinksRepo, KeyValueRepo<DeepLinkId, StoredDeepLink> by createDelegate(database, json)
