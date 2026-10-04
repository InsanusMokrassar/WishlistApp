package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStorageCursor
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Removes expired password-purpose approvals without depending on SMTP or link opens.
 * @param links Optional store supporting committed consumption and finite scans.
 * @param json Production aggregated serializer; unknown and malformed payloads remain untouched.
 * @param nowEpochMillis Injectable expiry clock.
 */
class EmailPasswordChangeApprovalCleanup(
    private val links: DeepLinksService?,
    private val json: Json,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) {
    /** Lifecycle-owned worker, retained even after cancellation to prevent duplicate starts. */
    private var worker: Job? = null

    /** Starts at most one loop in the injected server scope, including SMTP-disabled installations. */
    @Synchronized
    fun start(scope: CoroutineScope) {
        if (worker != null || links?.supportsMaintenance != true) return
        worker = scope.launch {
            while (true) {
                try {
                    cleanupSweep()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    KSLog.w("Password approval cleanup scan failed")
                }
                delay(60_000L)
            }
        }
    }

    /** Traverses one finite high-water range with 100-row queries and independent row failures. */
    suspend fun cleanupSweep() {
        val service = links?.takeIf { it.supportsMaintenance } ?: return
        currentCoroutineContext().ensureActive()
        val bound = service.scanUpperBound() ?: return
        var cursor: DeepLinkStorageCursor? = null
        while (true) {
            currentCoroutineContext().ensureActive()
            val page = service.scanPage(cursor, bound, 100)
            if (page.records.isEmpty()) return
            for (record in page.records) {
                currentCoroutineContext().ensureActive()
                val info = try {
                    json.decodeFromString(DeepLinkHandlerInfo.serializer(), record.cursor.rawInfo)
                } catch (_: SerializationException) {
                    continue
                } catch (_: IllegalArgumentException) {
                    continue
                }
                val payload = info.value as? EmailPasswordChangePayload ?: continue
                if (info.handlerId != EmailPasswordChange.handlerId || payload.expiresAtEpochMillis > nowEpochMillis()) continue
                try {
                    service.consumeDeepLink(record.cursor.id, info)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    KSLog.w("Password approval cleanup deletion failed")
                }
            }
            cursor = page.nextCursor ?: return
            yield()
        }
    }
}
