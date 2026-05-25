package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC implementation of [WishlistItemRepo] backed by the `wishlist_items` table.
 *
 * Schema:
 * - `id` — BIGINT, autoincrement primary key → [WishlistItemId]
 * - `wishlist_id` — BIGINT → parent [WishlistId]
 * - `title` — TEXT
 * - `approx_price_int` — BIGINT NULL → [Amount.integerPart]
 * - `approx_price_dec` — BIGINT NULL → [Amount.decimalPart] stored as signed Long bits
 * - `price_units` — TEXT
 * - `links` — TEXT, JSON-encoded `List<String>`
 * - `description` — TEXT
 *
 * [Amount] is `null` when both price columns are `null`.
 * [Amount.decimalPart] ([ULong]) is stored as its [Long] bit pattern and reconstructed with [Long.toULong].
 *
 * @param database Exposed [Database] instance injected from Koin.
 * @param json [Json] instance injected from Koin (registered by features.common.common plugin) for links serialisation.
 */
class ExposedWishlistItemRepo(
    override val database: Database,
    private val json: Json
) : WishlistItemRepo, AbstractExposedCRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>(tableName = "wishlist_items") {
    private val idColumn = long("id").autoIncrement()
    private val wishlistIdColumn = long("wishlist_id")
    private val titleColumn = text("title")
    private val approxPriceIntColumn = long("approx_price_int").nullable()
    private val approxPriceDecColumn = long("approx_price_dec").nullable()
    private val priceUnitsColumn = text("price_units")
    private val linksColumn = text("links")
    private val descriptionColumn = text("description")

    override val primaryKey = PrimaryKey(idColumn)

    private val linksSerializer = ListSerializer(String.serializer())

    /** Returns an [Amount] from the current row, or `null` if either price column is absent. */
    private fun ResultRow.amountOrNull(): Amount? {
        val intPart = get(approxPriceIntColumn) ?: return null
        val decPart = get(approxPriceDecColumn) ?: return null
        return Amount(intPart, decPart.toULong())
    }

    override val ResultRow.asObject: RegisteredWishlistItem
        get() = RegisteredWishlistItem(
            id = WishlistItemId(get(idColumn)),
            wishlistId = WishlistId(get(wishlistIdColumn)),
            title = get(titleColumn),
            approximatePrice = amountOrNull(),
            priceUnits = get(priceUnitsColumn),
            links = json.decodeFromString(linksSerializer, get(linksColumn)),
            description = get(descriptionColumn)
        )

    override val ResultRow.asId: WishlistItemId
        get() = WishlistItemId(get(idColumn))

    override val selectById: (WishlistItemId) -> Op<Boolean> = { idColumn.eq(it.long) }

    override fun update(id: WishlistItemId?, value: NewWishlistItem, it: UpdateBuilder<Int>) {
        it[wishlistIdColumn] = value.wishlistId.long
        it[titleColumn] = value.title
        it[approxPriceIntColumn] = value.approximatePrice?.integerPart
        it[approxPriceDecColumn] = value.approximatePrice?.decimalPart?.toLong()
        it[priceUnitsColumn] = value.priceUnits
        it[linksColumn] = json.encodeToString(linksSerializer, value.links)
        it[descriptionColumn] = value.description
    }

    override fun InsertStatement<Number>.asObject(value: NewWishlistItem): RegisteredWishlistItem =
        RegisteredWishlistItem(
            id = WishlistItemId(this[idColumn]),
            wishlistId = value.wishlistId,
            title = value.title,
            approximatePrice = value.approximatePrice,
            priceUnits = value.priceUnits,
            links = value.links,
            description = value.description
        )

    /**
     * Queries `wishlist_items` filtered by `wishlist_id` column.
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return All items with matching `wishlist_id`.
     */
    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        transaction(db = database) {
            selectAll().where { wishlistIdColumn eq wishlistId.long }.map { it.asObject }
        }

    init { initTable() }
}
