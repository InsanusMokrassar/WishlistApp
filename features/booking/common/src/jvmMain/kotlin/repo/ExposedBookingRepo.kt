package dev.inmo.wishlist.features.booking.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.booking.common.models.BookingId
import dev.inmo.wishlist.features.booking.common.models.NewBooking
import dev.inmo.wishlist.features.booking.common.models.RegisteredBooking
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC implementation of [BookingRepo] backed by the `wishlist_item_bookings` table.
 *
 * Schema (`wishlist_item_bookings`):
 * - `id` — BIGINT, autoincrement primary key → [BookingId]
 * - `item_id` — BIGINT, UNIQUE index → reserved [WishlistItemId]
 * - `user_id` — BIGINT → booking owner [UserId]
 *
 * The unique index on `item_id` enforces the single-active-booking invariant at the database
 * level: a second concurrent insert for an already-booked item fails with a constraint
 * violation, which the service layer maps to a "already booked" conflict.
 *
 * The table name is kept identical to the pre-extraction implementation so existing booking data
 * is preserved after the booking feature was moved out of `features/wishlist`.
 *
 * @param database Exposed [Database] instance injected from Koin.
 */
class ExposedBookingRepo(
    override val database: Database
) : BookingRepo, AbstractExposedCRUDRepo<RegisteredBooking, BookingId, NewBooking>(tableName = "wishlist_item_bookings") {
    private val idColumn = long("id").autoIncrement()
    private val itemIdColumn = long("item_id").uniqueIndex()
    private val userIdColumn = long("user_id")

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredBooking
        get() = RegisteredBooking(
            id = BookingId(get(idColumn)),
            itemId = WishlistItemId(get(itemIdColumn)),
            userId = UserId(get(userIdColumn))
        )

    override val ResultRow.asId: BookingId
        get() = BookingId(get(idColumn))

    override val selectById: (BookingId) -> Op<Boolean> = { idColumn.eq(it.long) }

    init {
        initTable()
    }

    override fun update(id: BookingId?, value: NewBooking, it: UpdateBuilder<Int>) {
        it[itemIdColumn] = value.itemId.long
        it[userIdColumn] = value.userId.long
    }

    override fun InsertStatement<Number>.asObject(value: NewBooking): RegisteredBooking =
        RegisteredBooking(
            id = BookingId(this[idColumn]),
            itemId = value.itemId,
            userId = value.userId
        )

    /**
     * Resolves the single active booking of [itemId] by querying `item_id`.
     *
     * @param itemId Item whose booking to resolve.
     * @return The matching [RegisteredBooking], or `null` when the item is not booked.
     */
    override suspend fun getByItemId(itemId: WishlistItemId): RegisteredBooking? =
        transaction(db = database) {
            selectAll().where { itemIdColumn eq itemId.long }.map { it.asObject }.firstOrNull()
        }

    /**
     * Lists all bookings placed by [userId] by querying `user_id`.
     *
     * @param userId Booker whose bookings to list.
     * @return Matching bookings; empty when the user has booked nothing.
     */
    override suspend fun getByUserId(userId: UserId): List<RegisteredBooking> =
        transaction(db = database) {
            selectAll().where { userIdColumn eq userId.long }.map { it.asObject }
        }
}
