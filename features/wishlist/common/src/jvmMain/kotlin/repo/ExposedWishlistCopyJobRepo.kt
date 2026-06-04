package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobStatus
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC implementation of [WishlistCopyJobRepo] backed by the `wishlist_copy_jobs` table.
 *
 * Persisting the queue here is what lets queued whole-wishlist copies survive a process restart:
 * on startup the worker re-scans [getUnfinished] and resumes processing idempotently.
 *
 * Schema (`wishlist_copy_jobs`):
 * - `id` — BIGINT, autoincrement primary key → [WishlistCopyJobId]
 * - `source_wishlist_id` — BIGINT → [WishlistId] of the wishlist being copied
 * - `recipient_user_id` — BIGINT → [UserId] receiving the copy
 * - `status` — TEXT holding the [WishlistCopyJobStatus] enum name
 *
 * @param database Exposed [Database] instance injected from Koin.
 */
class ExposedWishlistCopyJobRepo(
    override val database: Database
) : WishlistCopyJobRepo, AbstractExposedCRUDRepo<RegisteredWishlistCopyJob, WishlistCopyJobId, NewWishlistCopyJob>(
    tableName = "wishlist_copy_jobs"
) {
    private val idColumn = long("id").autoIncrement()
    private val sourceWishlistIdColumn = long("source_wishlist_id")
    private val recipientUserIdColumn = long("recipient_user_id")
    private val statusColumn = text("status")

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredWishlistCopyJob
        get() = RegisteredWishlistCopyJob(
            id = WishlistCopyJobId(get(idColumn)),
            sourceWishlistId = WishlistId(get(sourceWishlistIdColumn)),
            recipientUserId = UserId(get(recipientUserIdColumn)),
            status = WishlistCopyJobStatus.valueOf(get(statusColumn))
        )

    override val ResultRow.asId: WishlistCopyJobId
        get() = WishlistCopyJobId(get(idColumn))

    override val selectById: (WishlistCopyJobId) -> Op<Boolean> = { idColumn.eq(it.long) }

    init {
        initTable()
    }

    override fun update(id: WishlistCopyJobId?, value: NewWishlistCopyJob, it: UpdateBuilder<Int>) {
        it[sourceWishlistIdColumn] = value.sourceWishlistId.long
        it[recipientUserIdColumn] = value.recipientUserId.long
        it[statusColumn] = value.status.name
    }

    override fun InsertStatement<Number>.asObject(value: NewWishlistCopyJob): RegisteredWishlistCopyJob =
        RegisteredWishlistCopyJob(
            id = WishlistCopyJobId(this[idColumn]),
            sourceWishlistId = value.sourceWishlistId,
            recipientUserId = value.recipientUserId,
            status = value.status
        )

    /**
     * Queries `wishlist_copy_jobs` for rows whose `status` is one of the non-terminal values.
     *
     * @return Jobs still pending or in progress.
     */
    override suspend fun getUnfinished(): List<RegisteredWishlistCopyJob> =
        transaction(db = database) {
            selectAll().where {
                statusColumn inList listOf(
                    WishlistCopyJobStatus.Pending.name,
                    WishlistCopyJobStatus.InProgress.name
                )
            }.map { it.asObject }
        }
}
