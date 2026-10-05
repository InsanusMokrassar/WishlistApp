package dev.inmo.wishlist.features.deeplinks.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Retains the original table and publishes removals only after the caller's transaction commits.
 * @param database JDBC database owning the existing deeplinks table.
 */
private class RawDeepLinksRepo(database: Database) : ExposedKeyValueRepo<String, String>(
    database, { text("deeplink_id") }, { text("handler_info_json") }, "deeplinks"
) {
    /** Emits the existing removal flow after a successful committed conditional deletion. */
    suspend fun removed(id: String) { _onValueRemoved.emit(id) }
}

/** Maps the unchanged generic key/value operations using the aggregated serializer.
 * @param raw Existing raw key/value repository.
 * @param json Aggregated handler serializer.
 */
private fun mapped(raw: RawDeepLinksRepo, json: Json): KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> =
    raw.withMapper<DeepLinkId, DeepLinkHandlerInfo, String, String>(
        keyFromToTo = { string },
        valueFromToTo = { json.encodeToString(DeepLinkHandlerInfo.serializer(), this) },
        keyToToFrom = { DeepLinkId(this) },
        valueToToFrom = { json.decodeFromString(DeepLinkHandlerInfo.serializer(), this) }
    )

/** Persistent generic deeplinks plus committed exact-record consumption and bounded raw scans.
 * @param database JDBC database; consumption does not include any password write.
 * @param json Existing aggregated serializer for semantic expected-record comparison.
 * @param raw Original generic table delegate shared by mapped operations and maintenance.
 */
class ExposedDeepLinksRepo private constructor(
    private val database: Database,
    private val json: Json,
    private val raw: RawDeepLinksRepo
) : MaintainableDeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by mapped(raw, json) {
    /** Opens the existing table without changing its composite key or serialized representation. */
    constructor(database: Database, json: Json) : this(database, json, RawDeepLinksRepo(database))

    /** Returns true only after one exact observed record deletion commits; failed transactions throw. */
    override suspend fun consumeIfEquals(id: DeepLinkId, expectedInfo: DeepLinkHandlerInfo): Boolean {
        val consumed = transaction(database) {
            val rows = raw.selectAll().where { raw.keyColumn eq id.string }.limit(2).toList()
            if (rows.size != 1) return@transaction false
            val observed = rows.single()[raw.valueColumn]
            val decoded = try {
                json.decodeFromString(DeepLinkHandlerInfo.serializer(), observed)
            } catch (_: SerializationException) {
                return@transaction false
            } catch (_: IllegalArgumentException) {
                return@transaction false
            }
            if (decoded != expectedInfo) return@transaction false
            raw.deleteWhere { (raw.keyColumn eq id.string) and (raw.valueColumn eq observed) } == 1
        }
        if (consumed) raw.removed(id.string)
        return consumed
    }

    /** Captures the same database ordering used by subsequent keyset pages. */
    override suspend fun scanUpperBound(): DeepLinkStorageCursor? = transaction(database) {
        raw.selectAll().orderBy(raw.keyColumn to SortOrder.DESC, raw.valueColumn to SortOrder.DESC)
            .limit(1).singleOrNull()?.let { DeepLinkStorageCursor(DeepLinkId(it[raw.keyColumn]), it[raw.valueColumn]) }
    }

    /** Queries one bounded page without OFFSET, decoding, or a long-lived sweep transaction. */
    override suspend fun scanPage(afterExclusive: DeepLinkStorageCursor?, throughInclusive: DeepLinkStorageCursor, limit: Int): DeepLinkStoragePage {
        require(limit in 1..100) { "DeepLink page limit must be within 1..100" }
        return transaction(database) {
            val upper = (raw.keyColumn less throughInclusive.id.string) or
                ((raw.keyColumn eq throughInclusive.id.string) and (raw.valueColumn lessEq throughInclusive.rawInfo))
            val predicate = afterExclusive?.let {
                upper and ((raw.keyColumn greater it.id.string) or
                    ((raw.keyColumn eq it.id.string) and (raw.valueColumn greater it.rawInfo)))
            } ?: upper
            DeepLinkStoragePage(raw.selectAll().where { predicate }
                .orderBy(raw.keyColumn to SortOrder.ASC, raw.valueColumn to SortOrder.ASC).limit(limit)
                .map { DeepLinkStorageRecord(DeepLinkStorageCursor(DeepLinkId(it[raw.keyColumn]), it[raw.valueColumn])) })
        }
    }
}
