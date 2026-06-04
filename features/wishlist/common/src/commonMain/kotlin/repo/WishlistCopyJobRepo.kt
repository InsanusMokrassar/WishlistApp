package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobId

/**
 * Read-only repository for [RegisteredWishlistCopyJob] entities.
 *
 * Adds [getUnfinished] so the background copy worker can resume jobs left in a non-terminal status
 * after a server restart.
 */
interface ReadWishlistCopyJobRepo : ReadCRUDRepo<RegisteredWishlistCopyJob, WishlistCopyJobId> {
    /**
     * Returns all jobs that have not reached a terminal status
     * ([dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobStatus.Pending] or
     * [dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobStatus.InProgress]).
     *
     * @return Unfinished jobs to (re)process; empty list when none remain.
     */
    suspend fun getUnfinished(): List<RegisteredWishlistCopyJob>
}

/**
 * Write-only repository for [RegisteredWishlistCopyJob] entities.
 *
 * Accepts [NewWishlistCopyJob] as the input type for create and update operations.
 */
interface WriteWishlistCopyJobRepo : WriteCRUDRepo<RegisteredWishlistCopyJob, WishlistCopyJobId, NewWishlistCopyJob>

/**
 * Full CRUD repository for the persistent whole-wishlist copy job queue.
 *
 * Combines [ReadWishlistCopyJobRepo] and [WriteWishlistCopyJobRepo]; backed on JVM by an Exposed
 * table so queued jobs survive process restarts.
 */
interface WishlistCopyJobRepo :
    ReadWishlistCopyJobRepo,
    WriteWishlistCopyJobRepo,
    CRUDRepo<RegisteredWishlistCopyJob, WishlistCopyJobId, NewWishlistCopyJob>
