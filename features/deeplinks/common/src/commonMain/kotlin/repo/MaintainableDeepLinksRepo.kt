package dev.inmo.wishlist.features.deeplinks.common.repo

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/** Opaque database-order boundary; raw content must remain private to storage maintenance.
 * @param id Stored key.
 * @param rawInfo Exact persisted JSON used as the second ordering component.
 */
data class DeepLinkStorageCursor(val id: DeepLinkId, val rawInfo: String)

/** One opaque scanned row, including undecodable values.
 * @param cursor Exact stored key/value pair, also usable as a continuation boundary.
 */
data class DeepLinkStorageRecord(val cursor: DeepLinkStorageCursor)

/** A bounded ordered page whose continuation includes every scanned row.
 * @param records Rows in ascending database key/value order.
 */
data class DeepLinkStoragePage(val records: List<DeepLinkStorageRecord>) {
    /** Last scanned raw row; decoding failures cannot prevent progress. */
    val nextCursor: DeepLinkStorageCursor? get() = records.lastOrNull()?.cursor
}

/** Optional persistent compare-and-delete and bounded enumeration capability.
 * Implementations must grant consumption only after exactly one matching deletion commits.
 */
interface MaintainableDeepLinksRepo : DeepLinksRepo {
    /** Consumes a semantically equal unambiguous record, matching the observed raw value at deletion. */
    suspend fun consumeIfEquals(id: DeepLinkId, expectedInfo: DeepLinkHandlerInfo): Boolean
    /** Captures the greatest stored key/value pair in database ordering. */
    suspend fun scanUpperBound(): DeepLinkStorageCursor?
    /** Reads at most [limit] rows after the cursor through the inclusive captured bound.
     * @param afterExclusive Previous page's last raw row, or null for the first page.
     * @param throughInclusive Finite high-water mark captured at sweep start.
     * @param limit Page size within 1..100.
     */
    suspend fun scanPage(afterExclusive: DeepLinkStorageCursor?, throughInclusive: DeepLinkStorageCursor, limit: Int): DeepLinkStoragePage
}
