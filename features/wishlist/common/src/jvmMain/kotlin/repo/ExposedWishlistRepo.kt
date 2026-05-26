package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC implementation of [WishlistRepo] backed by the `wishlists` table.
 *
 * Schema:
 * - `id` — BIGINT, autoincrement primary key → [WishlistId]
 * - `user_id` — BIGINT → [UserId]
 * - `title` — TEXT → display name
 *
 * @param database Exposed [Database] instance injected from Koin.
 */
class ExposedWishlistRepo(
    override val database: Database
) : WishlistRepo, AbstractExposedCRUDRepo<RegisteredWishlist, WishlistId, NewWishlist>(tableName = "wishlists") {
    private val idColumn = long("id").autoIncrement()
    private val userIdColumn = long("user_id")
    private val titleColumn = text("title")

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredWishlist
        get() = RegisteredWishlist(
            id = WishlistId(get(idColumn)),
            userId = UserId(get(userIdColumn)),
            title = get(titleColumn)
        )

    override val ResultRow.asId: WishlistId
        get() = WishlistId(get(idColumn))

    override val selectById: (WishlistId) -> Op<Boolean> = { idColumn.eq(it.long) }

    init {
        initTable()
    }

    override fun update(id: WishlistId?, value: NewWishlist, it: UpdateBuilder<Int>) {
        it[userIdColumn] = value.userId.long
        it[titleColumn] = value.title
    }

    override fun InsertStatement<Number>.asObject(value: NewWishlist): RegisteredWishlist =
        RegisteredWishlist(
            id = WishlistId(this[idColumn]),
            userId = value.userId,
            title = value.title
        )

    /**
     * Queries `wishlists` filtered by `user_id` column.
     *
     * @param userId Owner to filter by.
     * @return All wishlists with matching `user_id`.
     */
    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        transaction(db = database) {
            selectAll().where { userIdColumn eq userId.long }.map { it.asObject }
        }
}
