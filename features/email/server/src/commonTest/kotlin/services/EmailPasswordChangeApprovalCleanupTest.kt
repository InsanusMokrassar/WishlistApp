package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.*
import dev.inmo.wishlist.features.deeplinks.common.repo.*
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.models.*
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.common.common.Plugin as CommonPlugin
import dev.inmo.wishlist.features.email.server.Plugin
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import kotlin.test.*

/** Finite sweep, purpose isolation, retry cadence, and lifecycle regression proof. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailPasswordChangeApprovalCleanupTest {
    /** Resolves exactly the production aggregated payload serializers without resolving optional SMTP. */
    private fun productionJson(): Json {
        val application = KoinApplication.init().modules(module {
            with(CommonPlugin) { setupDI(JsonObject(emptyMap())) }
            with(Plugin) { setupDI(buildJsonObject { put("publicHttpOrigin", "https://wishlist.example") }) }
        })
        return application.koin.get<Json>().also { application.close() }
    }

    /** Raw bounded persistence double; the mutex implements only the test store's atomic contract.
     * @param json Production payload decoder used for exact semantic conditional consumption.
     * @param delegate Unused generic operations remain compatible with ordinary deeplink callers.
     */
    private class RawRepo(
        private val json: Json,
        private val delegate: MapKeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> = MapKeyValueRepo(),
    ) : MaintainableDeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by delegate {
        /** Opaque persisted population, including malformed rows. */
        val rows = mutableMapOf<DeepLinkStorageCursor, Unit>()
        /** Test-only atomic deletion lock shared by multiple cleanup instances. */
        private val mutex = Mutex()
        /** Ordered per-query limits retained for bounded-work assertions. */
        val pageLimits = mutableListOf<Int>()
        /** Number of high-water queries, including failed attempts. */
        var starts = 0
        /** Optional scan failure used to prove normal backoff. */
        var scanFailure = false
        /** Keys whose first deletion fails before mutation. */
        val failOnce = mutableSetOf<DeepLinkId>()
        /** Keys of committed conditional removals. */
        val removals = mutableListOf<DeepLinkId>()
        /** Optional concurrent mutation seam executed after each page is captured. */
        var afterPage: (() -> Unit)? = null
        /** Database-order analogue for deterministic raw key/value fixture rows. */
        private val order = compareBy<DeepLinkStorageCursor>({ it.id.string }, { it.rawInfo })
        /** Persists an opaque raw row without decoding. */
        fun seed(id: String, raw: String) { rows[DeepLinkStorageCursor(DeepLinkId(id), raw)] = Unit }
        /** Returns the greatest complete pair, including malformed payloads. */
        override suspend fun scanUpperBound(): DeepLinkStorageCursor? {
            starts++
            if (scanFailure) error("scan failure")
            return rows.keys.maxWithOrNull(order)
        }
        /** Captures bounded ordered raw rows before a controlled concurrent mutation. */
        override suspend fun scanPage(afterExclusive: DeepLinkStorageCursor?, throughInclusive: DeepLinkStorageCursor, limit: Int): DeepLinkStoragePage {
            require(limit in 1..100)
            pageLimits += limit
            val records = rows.keys.sortedWith(order).filter {
                (afterExclusive == null || order.compare(it, afterExclusive) > 0) && order.compare(it, throughInclusive) <= 0
            }.take(limit).map(::DeepLinkStorageRecord)
            afterPage?.invoke()
            return DeepLinkStoragePage(records)
        }
        /** Deletes only one matching row and records only committed fake removals. */
        override suspend fun consumeIfEquals(id: DeepLinkId, expectedInfo: DeepLinkHandlerInfo): Boolean = mutex.withLock {
            if (failOnce.remove(id)) error("delete failure")
            val matching = rows.keys.filter { it.id == id }
            if (matching.size != 1) return@withLock false
            val row = matching.single()
            val actual = runCatching { json.decodeFromString(DeepLinkHandlerInfo.serializer(), row.rawInfo) }.getOrNull()
            if (actual != expectedInfo) return@withLock false
            rows.remove(row)
            removals += id
            true
        }
    }

    /** Encodes the unchanged server-only approval representation. */
    private fun password(json: Json, expiry: Long, handler: DeepLinkHandlerId = EmailPasswordChange.handlerId): String =
        json.encodeToString(DeepLinkHandlerInfo.serializer(), DeepLinkHandlerInfo(handler,
            EmailPasswordChangePayload(UserId(7), Email("owner@example.com"), expiry, "credential-state")))

    /** Immediate startup cleanup reaches more than two pages and preserves every unrelated/undecodable purpose. */
    @Test
    fun startupSweepsUnopenedLegacyRowsWithBoundedProgressAndExactExpiry() = runTest {
        val json = productionJson()
        val repo = RawRepo(json)
        repeat(250) { repo.seed("expired-%03d".format(it), password(json, 1_000)) }
        repo.seed("expired-099-malformed", "not-json")
        repo.seed("unknown", "{\"handlerId\":\"unknown\",\"value\":[\"missing.serializer\",{}]}")
        repo.seed("future", password(json, 1_001))
        repo.seed("wrong-handler", password(json, 0, DeepLinkHandlerId("other")))
        repo.seed("verification", json.encodeToString(DeepLinkHandlerInfo.serializer(), DeepLinkHandlerInfo(
            EmailPasswordChange.handlerId, EmailVerificationPayload(UserId(7), Email("owner@example.com")))))
        val cleanup = EmailPasswordChangeApprovalCleanup(DeepLinksService(repo, emptyList()), json) { 1_000 }
        cleanup.start(backgroundScope)
        cleanup.start(backgroundScope)
        runCurrent()
        assertEquals(250, repo.removals.size)
        assertEquals(1, repo.starts)
        assertEquals(5, repo.rows.size)
        assertTrue(repo.pageLimits.size >= 3)
        assertTrue(repo.pageLimits.all { it == 100 })
        advanceTimeBy(59_999)
        runCurrent()
        assertEquals(1, repo.starts)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, repo.starts)
        backgroundScope.cancel()
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(2, repo.starts)
        // Restarted server performs immediate expiry observation, including previously unexpired approvals.
        val restarted = EmailPasswordChangeApprovalCleanup(DeepLinksService(repo, emptyList()), json) { 1_001 }
        restarted.cleanupSweep()
        assertTrue(DeepLinkId("future") in repo.removals)
    }

    /** Captured bounds stop inserts extending a sweep; the next reset reaches behind-cursor and greater rows. */
    @Test
    fun finiteSweepRetriesFailuresAndPreservesConcurrentPurposeReplacement() = runTest {
        val json = productionJson()
        val repo = RawRepo(json)
        repeat(205) { repo.seed("m-%03d".format(it), password(json, 0)) }
        repo.failOnce += DeepLinkId("m-001")
        var inserted = false
        repo.afterPage = {
            if (!inserted) {
                inserted = true
                repo.seed("a-behind", password(json, 0))
                repo.seed("z-after", password(json, 0))
                repo.rows.keys.filter { it.id == DeepLinkId("m-002") }.forEach { repo.rows.remove(it) }
                repo.seed("m-002", password(json, 0, DeepLinkHandlerId("other")))
                repo.rows.keys.filter { it.id == DeepLinkId("m-003") }.forEach { repo.rows.remove(it) }
            }
        }
        val links = DeepLinksService(repo, emptyList())
        val cleanup = EmailPasswordChangeApprovalCleanup(links, json) { 0 }
        cleanup.cleanupSweep()
        assertFalse(DeepLinkId("a-behind") in repo.removals)
        assertFalse(DeepLinkId("z-after") in repo.removals)
        assertFalse(DeepLinkId("m-001") in repo.removals)
        assertFalse(DeepLinkId("m-002") in repo.removals)
        coroutineScope {
            awaitAll(async { cleanup.cleanupSweep() }, async { EmailPasswordChangeApprovalCleanup(links, json) { 0 }.cleanupSweep() })
        }
        assertTrue(DeepLinkId("a-behind") in repo.removals)
        assertTrue(DeepLinkId("z-after") in repo.removals)
        assertTrue(DeepLinkId("m-001") in repo.removals)
        assertEquals(repo.removals.size, repo.removals.distinct().size)
        assertEquals(listOf(DeepLinkId("m-002")), repo.rows.keys.map { it.id })
    }

    /** Scan failures use the 60-second cadence and optional/unsupported stores create no worker. */
    @Test
    fun scanFailureBacksOffAndAbsentMaintenanceIsIdle() = runTest {
        val json = productionJson()
        val repo = RawRepo(json).apply { scanFailure = true }
        val cleanup = EmailPasswordChangeApprovalCleanup(DeepLinksService(repo, emptyList()), json) { 0 }
        cleanup.start(backgroundScope)
        runCurrent()
        assertEquals(1, repo.starts)
        advanceTimeBy(59_999); runCurrent()
        assertEquals(1, repo.starts)
        repo.scanFailure = false
        repo.seed("old", password(json, 0))
        advanceTimeBy(1); runCurrent()
        assertEquals(2, repo.starts)
        assertEquals(listOf(DeepLinkId("old")), repo.removals)
        EmailPasswordChangeApprovalCleanup(null, json).start(backgroundScope)
        val generic = object : DeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo() {}
        EmailPasswordChangeApprovalCleanup(DeepLinksService(generic, emptyList()), json).start(backgroundScope)
        runCurrent()
        assertEquals(2, repo.starts)
    }
}
