package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Type-safe identifier for a [WishlistCopyJob]. Backed by a [Long] primary key. */
@Serializable
@JvmInline
value class WishlistCopyJobId(val long: Long)

/**
 * Lifecycle status of a queued whole-wishlist copy job.
 *
 * - [Pending] — created, not yet picked up by the worker.
 * - [InProgress] — currently being processed by the worker.
 * - [Done] — processing finished successfully.
 * - [Failed] — processing aborted (e.g. source wishlist disappeared).
 *
 * [Pending] and [InProgress] jobs are considered unfinished and are re-scanned/resumed on server
 * startup, which is safe because processing is idempotent.
 */
@Serializable
enum class WishlistCopyJobStatus {
    Pending,
    InProgress,
    Done,
    Failed
}

/**
 * Common interface for all wishlist-copy job variants.
 *
 * Sealed to allow exhaustive handling of [NewWishlistCopyJob] and [RegisteredWishlistCopyJob].
 */
@Serializable
sealed interface WishlistCopyJob {
    /** Source wishlist (owned by any user) that must be deep-copied. */
    val sourceWishlistId: WishlistId

    /** Recipient — the user into whose profile the copy is produced. Always the authenticated caller. */
    val recipientUserId: UserId

    /** Current processing status of the job. */
    val status: WishlistCopyJobStatus
}

/**
 * Wishlist-copy job not yet persisted — used as input to the queue repository create operation.
 *
 * @property sourceWishlistId Wishlist to copy.
 * @property recipientUserId Owner of the produced copy.
 * @property status Initial status; defaults to [WishlistCopyJobStatus.Pending].
 */
@Serializable
data class NewWishlistCopyJob(
    override val sourceWishlistId: WishlistId,
    override val recipientUserId: UserId,
    override val status: WishlistCopyJobStatus = WishlistCopyJobStatus.Pending
) : WishlistCopyJob

/**
 * Wishlist-copy job already persisted in the queue table.
 *
 * @property id Unique persistent identifier assigned by the server.
 * @property sourceWishlistId Wishlist to copy.
 * @property recipientUserId Owner of the produced copy.
 * @property status Current processing status.
 */
@Serializable
data class RegisteredWishlistCopyJob(
    val id: WishlistCopyJobId,
    override val sourceWishlistId: WishlistId,
    override val recipientUserId: UserId,
    override val status: WishlistCopyJobStatus
) : WishlistCopyJob
