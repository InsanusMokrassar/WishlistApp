package dev.inmo.wishlist.features.deeplinks.server.services

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Verifies dispatcher behavior for missing, unhandled, ordinary, and redirect results. */
class DeepLinksServiceTest {
    /** In-memory deeplink persistence for dispatcher tests. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /**
     * Persistence double that commits one record, then loses its response to exercise exact cleanup.
     *
     * @param delegate Actual map persistence retaining records before the simulated lost response.
     */
    private class CommitThenThrowDeepLinksRepo(
        /** Actual map persistence retaining records before the simulated lost response. */
        private val delegate: MapKeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> = MapKeyValueRepo(),
    ) : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by delegate {

        /** Identifiers passed through the service's cleanup boundary. */
        val unsetIds = mutableListOf<DeepLinkId>()

        /** Commits first, then throws as if persistence accepted the write but the caller lost confirmation. */
        override suspend fun set(toSet: Map<DeepLinkId, DeepLinkHandlerInfo>) {
            delegate.set(toSet)
            throw IllegalStateException("persistence response lost")
        }

        /** Records and performs only the exact service-requested cleanup. */
        override suspend fun unset(toUnset: List<DeepLinkId>) {
            unsetIds += toUnset
            delegate.unset(toUnset)
        }

    }

    /** Configurable handler fixture whose result is returned unchanged. */
    private class FakeHandler(
        override val id: DeepLinkHandlerId,
        private val result: HandleResult.Handled?,
    ) : DeepLinkHandler {
        /** Returns the configured outcome without interpreting the stored payload. */
        override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled? = result
    }

    /** A nonexistent identifier never reaches a handler and returns the not-found result. */
    /** Verifies missing records map to NotFound without handler invocation. */
    @Test
    fun missingLinkReturnsNotFound() = runTest {
        val service = DeepLinksService(FakeDeepLinksRepo(), emptyList())

        assertEquals(HandleResult.NotFound, service.handle(DeepLinkId("missing")))
    }

    /** Unknown handlers and nullable handler outcomes both remain unhandled. */
    /** Verifies links with no matching handler remain unhandled. */
    @Test
    fun unclaimedLinksReturnUnhandled() = runTest {
        val repo = FakeDeepLinksRepo()
        val unknownId = DeepLinkId("unknown-handler")
        val nullId = DeepLinkId("null-result")
        repo.set(unknownId, DeepLinkHandlerInfo(DeepLinkHandlerId("unknown"), "value"))
        repo.set(nullId, DeepLinkHandlerInfo(DeepLinkHandlerId("null"), "value"))
        val service = DeepLinksService(
            repo,
            listOf(FakeHandler(DeepLinkHandlerId("null"), null)),
        )

        assertEquals(HandleResult.Unhandled, service.handle(unknownId))
        assertEquals(HandleResult.Unhandled, service.handle(nullId))
    }

    /** Successful outcomes are returned exactly, including the redirect destination. */
    /** Verifies handler results are returned without dispatcher rewriting. */
    @Test
    fun handledResultsArePreserved() = runTest {
        val repo = FakeDeepLinksRepo()
        val commonId = DeepLinkId("common")
        val redirectId = DeepLinkId("redirect")
        val commonHandlerId = DeepLinkHandlerId("common-handler")
        val redirectHandlerId = DeepLinkHandlerId("redirect-handler")
        repo.set(commonId, DeepLinkHandlerInfo(commonHandlerId, "value"))
        repo.set(redirectId, DeepLinkHandlerInfo(redirectHandlerId, "value"))
        val service = DeepLinksService(
            repo,
            listOf(
                FakeHandler(commonHandlerId, HandleResult.Handled.Common),
                FakeHandler(redirectHandlerId, HandleResult.Handled.Redirect("/destination")),
            ),
        )

        assertEquals(HandleResult.Handled.Common, service.handle(commonId))
        assertEquals(HandleResult.Handled.Redirect("/destination"), service.handle(redirectId))
    }

    /** Duplicate handler identifiers fail immediately instead of shadowing one registration. */
    /** Verifies duplicate handler identifiers fail during service construction. */
    @Test
    fun duplicateHandlerIdsFailAtConstruction() {
        val id = DeepLinkHandlerId("duplicate")

        assertFailsWith<IllegalArgumentException> {
            DeepLinksService(FakeDeepLinksRepo(), listOf(FakeHandler(id, null), FakeHandler(id, null)))
        }
    }

    /** A persistence exception after commit triggers non-cancellable removal of only the minted UUID. */
    /** Verifies mint failures remove exactly the allocated deeplink. */
    @Test
    fun mintFailureCleansExactlyTheAllocatedLink() = runTest {
        val repo = CommitThenThrowDeepLinksRepo()
        val service = DeepLinksService(repo, emptyList())

        assertFailsWith<IllegalStateException> {
            service.createDeepLink(DeepLinkHandlerId("password-change"), "value")
        }

        assertEquals(1, repo.unsetIds.size)
        assertEquals(repo.unsetIds.single(), repo.unsetIds.distinct().single())
        assertEquals(emptyMap(), repo.getAll())
    }
    /** The optional capability never authorizes an ordinary store through read-plus-unset fallback. */
    @Test
    fun unsupportedStoreCannotConsume() = runTest {
        val repo = FakeDeepLinksRepo()
        val id = DeepLinkId("approval")
        val info = DeepLinkHandlerInfo(DeepLinkHandlerId("password"), "value")
        repo.set(id, info)
        val service = DeepLinksService(repo, emptyList())
        kotlin.test.assertFalse(service.supportsMaintenance)
        kotlin.test.assertFalse(service.consumeDeepLink(id, info))
        assertEquals(info, service.getDeepLinkInfo(id))
        kotlin.test.assertNull(service.scanUpperBound())
    }

    /** Capability wrappers forward exact record, cursors, high-water bound, and page limit without interpretation. */
    @Test
    fun maintainableStoreReceivesExactConsumeAndScanArguments() = runTest {
        val id = DeepLinkId("approval")
        val expected = DeepLinkHandlerInfo(DeepLinkHandlerId("password"), "value")
        val bound = dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStorageCursor(id, "opaque")
        val page = dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStoragePage(listOf(dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStorageRecord(bound)))
        var answer = false
        val repo = object : dev.inmo.wishlist.features.deeplinks.common.repo.MaintainableDeepLinksRepo,
            dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo() {
            override suspend fun consumeIfEquals(id: DeepLinkId, expectedInfo: DeepLinkHandlerInfo): Boolean {
                assertEquals(bound.id, id); assertEquals(expected, expectedInfo); return answer
            }
            override suspend fun scanUpperBound() = bound
            override suspend fun scanPage(afterExclusive: dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStorageCursor?, throughInclusive: dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStorageCursor, limit: Int): dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinkStoragePage {
                assertEquals(bound, afterExclusive); assertEquals(bound, throughInclusive); assertEquals(100, limit); return page
            }
        }
        val service = DeepLinksService(repo, emptyList())
        kotlin.test.assertTrue(service.supportsMaintenance)
        kotlin.test.assertFalse(service.consumeDeepLink(id, expected))
        answer = true
        kotlin.test.assertTrue(service.consumeDeepLink(id, expected))
        assertEquals(bound, service.scanUpperBound())
        assertEquals(page, service.scanPage(bound, bound, 100))
    }

}
