package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.ExposedRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC implementation of [WishlistItemRepo] backed by the `wishlist_items` table.
 *
 * Schema (`wishlist_items`):
 * - `id` — BIGINT, autoincrement primary key → [WishlistItemId]
 * - `wishlist_id` — BIGINT → parent [WishlistId]
 * - `title` — TEXT
 * - `approx_price_int` — BIGINT NULL → [Amount.integerPart]
 * - `approx_price_dec` — BIGINT NULL → [Amount.decimalPart] stored as signed Long bits
 * - `price_units` — TEXT
 * - `description` — TEXT
 *
 * Links are stored in a separate `wishlist_item_links` table (see [linksTable]),
 * private to this class and managed exclusively here. On item delete, the DB cascades
 * removal of the corresponding link rows automatically.
 *
 * [Amount] is `null` when both price columns are `null`.
 * [Amount.decimalPart] ([ULong]) is stored as its [Long] bit pattern and reconstructed with [Long.toULong].
 *
 * @param database Exposed [Database] instance injected from Koin.
 */
class ExposedWishlistItemRepo(
    override val database: Database,
) : WishlistItemRepo, AbstractExposedCRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>(tableName = "wishlist_items") {
    private val idColumn = long("id").autoIncrement()
    private val wishlistIdColumn = long("wishlist_id")
    private val titleColumn = text("title")
    private val approxPriceIntColumn = long("approx_price_int").nullable()
    private val approxPriceDecColumn = long("approx_price_dec").nullable()
    private val priceUnitsColumn = text("price_units")
    private val descriptionColumn = text("description")

    override val primaryKey = PrimaryKey(idColumn)

    private inner class WishlistItemsLinks(override val database: Database) : Table("wishlist_item_links"), ExposedRepo {
        val itemId = long("item_id").references(idColumn, onDelete = ReferenceOption.CASCADE)
        val link = text("link")
        override val primaryKey = PrimaryKey(itemId, link)

        init {
            this@WishlistItemsLinks.initTable()
        }
    }
    /**
     * Internal table holding one row per link per wishlist item.
     * Never accessed outside [ExposedWishlistItemRepo]. Cascade-deletes when the parent item is removed.
     *
     * Schema (`wishlist_item_links`):
     * - `item_id` — BIGINT FK → `wishlist_items.id` ON DELETE CASCADE
     * - `link` — TEXT
     * - PK: (item_id, link)
     */
    private val linksTable = WishlistItemsLinks(database)

    /** Returns an [Amount] from the current row, or `null` if either price column is absent. */
    private fun ResultRow.amountOrNull(): Amount? {
        val intPart = get(approxPriceIntColumn) ?: return null
        val decPart = get(approxPriceDecColumn) ?: return null
        return Amount(intPart, decPart.toULong())
    }

    /**
     * Fetches all link strings for [itemId] from [linksTable]. Must be called within an active transaction.
     *
     * @param itemId Raw Long id of the parent item.
     * @return Ordered list of link strings.
     */
    private fun linksFor(itemId: Long): List<String> =
        linksTable.selectAll().where { linksTable.itemId eq itemId }.map { it[linksTable.link] }

    override val ResultRow.asObject: RegisteredWishlistItem
        get() {
            val id = get(idColumn)
            return RegisteredWishlistItem(
                id = WishlistItemId(id),
                wishlistId = WishlistId(get(wishlistIdColumn)),
                title = get(titleColumn),
                approximatePrice = amountOrNull(),
                priceUnits = get(priceUnitsColumn),
                links = linksFor(id),
                description = get(descriptionColumn)
            )
        }

    override val ResultRow.asId: WishlistItemId
        get() = WishlistItemId(get(idColumn))

    override val selectById: (WishlistItemId) -> Op<Boolean> = { idColumn.eq(it.long) }

    init {
        initTable()
    }

    /**
     * Fills [it] with scalar columns from [value]. When [id] is non-null (update path),
     * also replaces all link rows in [linksTable] for that item.
     *
     * @param id Non-null on update, null on insert (links for insert are written in [InsertStatement.asObject]).
     * @param value New item data.
     * @param it Builder targeting the `wishlist_items` row.
     */
    override fun update(id: WishlistItemId?, value: NewWishlistItem, it: UpdateBuilder<Int>) {
        it[wishlistIdColumn] = value.wishlistId.long
        it[titleColumn] = value.title
        it[approxPriceIntColumn] = value.approximatePrice?.integerPart
        it[approxPriceDecColumn] = value.approximatePrice?.decimalPart?.toLong()
        it[priceUnitsColumn] = value.priceUnits
        it[descriptionColumn] = value.description
        if (id != null) {
            linksTable.deleteWhere { linksTable.itemId eq id.long }
            value.links.forEach { link ->
                linksTable.insert { stmt ->
                    stmt[linksTable.itemId] = id.long
                    stmt[linksTable.link] = link
                }
            }
        }
    }

    /**
     * Constructs the [RegisteredWishlistItem] after a successful insert and writes
     * the item's links into [linksTable] within the same transaction.
     *
     * @param value Source data containing links to persist.
     * @return Fully populated [RegisteredWishlistItem] including the auto-generated id.
     */
    override fun InsertStatement<Number>.asObject(value: NewWishlistItem): RegisteredWishlistItem {
        val id = this[idColumn]
        value.links.forEach { link ->
            linksTable.insert { stmt ->
                stmt[linksTable.itemId] = id
                stmt[linksTable.link] = link
            }
        }
        return RegisteredWishlistItem(
            id = WishlistItemId(id),
            wishlistId = value.wishlistId,
            title = value.title,
            approximatePrice = value.approximatePrice,
            priceUnits = value.priceUnits,
            links = value.links,
            description = value.description
        )
    }

    /**
     * Queries `wishlist_items` filtered by `wishlist_id` column.
     *
     * @param wishlistId Parent wishlist to filter by.
     * @return All items with matching `wishlist_id`, each populated with links from [linksTable].
     */
    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        transaction(db = database) {
            selectAll().where { wishlistIdColumn eq wishlistId.long }.map { it.asObject }
        }
}
