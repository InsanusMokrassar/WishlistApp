package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.e
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.repos.update
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistCopyJob
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistCopyJobStatus
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.hasSameContentAs
import dev.inmo.wishlist.features.wishlist.common.models.toNewItem
import dev.inmo.wishlist.features.wishlist.server.repo.WishlistCopyJobRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Background queue service that deep-copies whole wishlists into recipient profiles.
 *
 * Resilience and concurrency model:
 * - **Persistent queue:** every requested copy is recorded as a [RegisteredWishlistCopyJob] row via
 *   [copyJobRepo] before any work starts, so a reload/crash never loses a request.
 * - **Resume on restart:** [start] re-scans [WishlistCopyJobRepo.getUnfinished] and re-submits those
 *   jobs for processing. Because [processJob] is idempotent (see below), re-running a partially-done
 *   job never produces duplicates.
 * - **Parallel processing:** submitted job ids flow through an unbounded [Channel]; the worker
 *   launches one child coroutine per job, bounded by a [Semaphore] so up to [maxParallelJobs] jobs
 *   progress concurrently.
 *
 * Idempotency of [processJob]:
 * 1. **Wishlist-by-name:** the recipient is searched for a wishlist whose title equals the source
 *    title; if found it is reused, otherwise a new one is created.
 * 2. **Item-existence:** for every source item, the recipient wishlist is checked for an item with
 *    identical content ([hasSameContentAs]); the item is copied only when absent.
 *
 * @param copyJobRepo Persistent queue of copy jobs.
 * @param wishlistRepo Repository of wishlists (source read + recipient read/create).
 * @param wishlistItemRepo Repository of wishlist items (source read + recipient read/create).
 * @param scope Coroutine scope the worker runs in; shared application scope from DI.
 * @param maxParallelJobs Maximum number of copy jobs processed concurrently.
 */
class WishlistCopyService(
    private val copyJobRepo: WishlistCopyJobRepo,
    private val wishlistRepo: WishlistRepo,
    private val wishlistItemRepo: WishlistItemRepo,
    private val scope: CoroutineScope,
    private val maxParallelJobs: Int = 4
) {
    private val jobsChannel = Channel<WishlistCopyJobId>(Channel.UNLIMITED)
    private val semaphore = Semaphore(maxParallelJobs)

    /**
     * Records a new copy job and submits it for processing.
     *
     * @param sourceWishlistId Wishlist to copy (may belong to any user).
     * @param recipientUserId Authenticated caller — owner of the produced copy.
     * @return The persisted [RegisteredWishlistCopyJob], or `null` when the repo returns no result.
     */
    suspend fun enqueue(sourceWishlistId: WishlistId, recipientUserId: UserId): RegisteredWishlistCopyJob? {
        val job = copyJobRepo.create(
            NewWishlistCopyJob(
                sourceWishlistId = sourceWishlistId,
                recipientUserId = recipientUserId,
                status = WishlistCopyJobStatus.Pending
            )
        ).firstOrNull() ?: return null
        jobsChannel.send(job.id)
        return job
    }

    /**
     * Starts the worker: resumes unfinished jobs left from a previous run, then drains the job
     * channel, processing up to [maxParallelJobs] jobs in parallel. Idempotent per process — invoke
     * once during server startup.
     */
    fun start() {
        scope.launchLoggingDropExceptions {
            copyJobRepo.getUnfinished().forEach { jobsChannel.send(it.id) }
        }
        scope.launchLoggingDropExceptions {
            for (jobId in jobsChannel) {
                scope.launchLoggingDropExceptions {
                    semaphore.withPermit { processJob(jobId) }
                }
            }
        }
    }

    /**
     * Deep-copies a single queued job idempotently (see class docs for the two existence checks).
     *
     * @param jobId Identifier of the job to process.
     */
    private suspend fun processJob(jobId: WishlistCopyJobId) {
        val job = copyJobRepo.getById(jobId) ?: return
        copyJobRepo.update(jobId, job.toNew(WishlistCopyJobStatus.InProgress))

        val source = wishlistRepo.getById(job.sourceWishlistId)
        if (source == null) {
            KSLog.e("WishlistCopyService") { "Source wishlist ${job.sourceWishlistId} gone; failing job $jobId" }
            copyJobRepo.update(jobId, job.toNew(WishlistCopyJobStatus.Failed))
            return
        }

        // Idempotency check 1 — wishlist-by-name: reuse a recipient wishlist with the same title.
        val target = wishlistRepo.getByUserId(job.recipientUserId)
            .firstOrNull { it.title == source.title }
            ?: wishlistRepo.create(
                NewWishlist(job.recipientUserId, source.title, source.defaultPriceUnits)
            ).firstOrNull()
        if (target == null) {
            KSLog.e("WishlistCopyService") { "Failed to obtain target wishlist for job $jobId" }
            copyJobRepo.update(jobId, job.toNew(WishlistCopyJobStatus.Failed))
            return
        }

        // Idempotency check 2 — item-existence: copy each source item only when absent in the target.
        val existingTargetItems = wishlistItemRepo.getByWishlistId(target.id)
        wishlistItemRepo.getByWishlistId(source.id).forEach { sourceItem ->
            val newItem = sourceItem.toNewItem(target.id)
            if (existingTargetItems.none { it.hasSameContentAs(newItem) }) {
                wishlistItemRepo.create(newItem)
            }
        }

        copyJobRepo.update(jobId, job.toNew(WishlistCopyJobStatus.Done))
    }

    /**
     * Builds a [NewWishlistCopyJob] preserving this job's source/recipient with a new [status].
     *
     * @param status New status to persist.
     * @return Update payload for [WishlistCopyJobRepo.update].
     */
    private fun RegisteredWishlistCopyJob.toNew(status: WishlistCopyJobStatus): NewWishlistCopyJob =
        NewWishlistCopyJob(sourceWishlistId, recipientUserId, status)
}
