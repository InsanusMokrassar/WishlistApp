package dev.inmo.wishlist.features.files.server.services

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.ktor.common.TemporalFileId
import dev.inmo.micro_utils.ktor.server.TemporalFilesRoutingConfigurator
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [TemporalFilesRoutingConfigurator.TemporalFilesUtilizer] that deletes temporal uploads which were
 * never finalized within [ttlMillis], bounding disk and in-memory growth caused by abandoned uploads.
 *
 * The utilizer shipped by default with [TemporalFilesRoutingConfigurator] performs no cleanup, so a
 * never-finalized upload would survive until process exit. This implementation records the first-seen
 * time of every temporal file id emitted by the configurator and, on each [checkIntervalMillis] tick,
 * removes from the shared map (and deletes from disk) every entry older than [ttlMillis].
 *
 * Finalized files are removed from the shared map by the configurator itself; their leftover timestamp
 * entries are dropped on the next sweep without touching disk.
 *
 * @param scope Scope the background sweep and the id-tracking collector run in.
 * @param ttlMillis Maximum lifetime of an unfinalized temporal file before it is purged.
 * @param checkIntervalMillis Delay between sweeps; defaults to one minute.
 */
class TimedTemporalFilesUtilizer(
    private val scope: CoroutineScope,
    private val ttlMillis: Long,
    private val checkIntervalMillis: Long = 60L * 1000L,
) : TemporalFilesRoutingConfigurator.TemporalFilesUtilizer {
    /**
     * Starts the id-tracking collector and the periodic sweep loop.
     *
     * @param filesMap Shared map of pending temporal files owned by the configurator.
     * @param filesMutex Mutex guarding [filesMap]; held only while removing a single entry.
     * @param onNewFileFlow Emits the id of every newly stored temporal file.
     * @return The [Job] running both the collector and the sweep loop.
     */
    override fun start(
        filesMap: MutableMap<TemporalFileId, MPPFile>,
        filesMutex: Mutex,
        onNewFileFlow: Flow<TemporalFileId>,
    ): Job {
        val firstSeenAtMillis = mutableMapOf<TemporalFileId, Long>()
        val firstSeenMutex = Mutex()
        return scope.launchLoggingDropExceptions {
            launchLoggingDropExceptions {
                onNewFileFlow.collect { id ->
                    firstSeenMutex.withLock { firstSeenAtMillis[id] = DateTime.now().unixMillisLong }
                }
            }
            while (isActive) {
                delay(checkIntervalMillis)
                val nowMillis = DateTime.now().unixMillisLong
                val expiredIds = firstSeenMutex.withLock {
                    firstSeenAtMillis.filterValues { nowMillis - it >= ttlMillis }.keys.toList()
                }
                for (id in expiredIds) {
                    filesMutex.withLock { filesMap.remove(id) }?.delete()
                    firstSeenMutex.withLock { firstSeenAtMillis.remove(id) }
                }
            }
        }
    }
}
