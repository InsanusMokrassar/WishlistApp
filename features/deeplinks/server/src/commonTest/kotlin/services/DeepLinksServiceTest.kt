package dev.inmo.wishlist.features.deeplinks.server.services

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
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

    /** Configurable handler fixture whose result is returned unchanged. */
    private class FakeHandler(
        override val id: DeepLinkHandlerId,
        private val result: HandleResult.Handled?,
    ) : DeepLinkHandler {
        /** Returns the configured outcome without interpreting the stored payload. */
        override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled? = result
    }

    /** A nonexistent identifier never reaches a handler and returns the not-found result. */
    @Test
    fun missingLinkReturnsNotFound() = runTest {
        val service = DeepLinksService(FakeDeepLinksRepo(), emptyList())

        assertEquals(HandleResult.NotFound, service.handle(DeepLinkId("missing")))
    }

    /** Unknown handlers and nullable handler outcomes both remain unhandled. */
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
    @Test
    fun duplicateHandlerIdsFailAtConstruction() {
        val id = DeepLinkHandlerId("duplicate")

        assertFailsWith<IllegalArgumentException> {
            DeepLinksService(FakeDeepLinksRepo(), listOf(FakeHandler(id, null), FakeHandler(id, null)))
        }
    }
}
