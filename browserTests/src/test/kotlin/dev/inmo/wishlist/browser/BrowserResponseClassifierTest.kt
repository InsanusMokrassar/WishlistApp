package dev.inmo.wishlist.browser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies that the browser collector rejects every console, page, and authorization error. */
class BrowserResponseClassifierTest {
    /** Generated-server origin used to exercise protected endpoint matching. */
    private val baseUrl = "http://127.0.0.1:32123"

    /** Rejects former anonymous bootstrap 401 responses instead of allowing them by phase or endpoint. */
    @Test
    fun rejectsFormerAnonymousBootstrap401() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordResponse(BrowserResponseDetails("GET", "$baseUrl/api/wishlist/getMy", 401))
        assertEquals(listOf("Unexpected HTTP 401: GET $baseUrl/api/wishlist/getMy"), collector.unexpectedErrors())
    }

    /** Rejects transformation diagnostics from unlocated and webpack-internal console sources. */
    @Test
    fun rejectsEveryTransformationConsoleError() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordConsoleError("NoTransformationFoundException")
        collector.recordConsoleError("NoTransformationFoundException")
        assertEquals(listOf("NoTransformationFoundException", "NoTransformationFoundException"), collector.unexpectedErrors())
    }

    /** Counts an exact protected request even when no response is available. */
    @Test
    fun countsProtectedOwnListRequestsWithoutResponse() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordRequest(BrowserRequestDetails("GET", "$baseUrl/api/wishlist/getMy"))
        assertEquals(1, collector.protectedOwnListRequests())
        assertTrue(collector.unexpectedErrors().isEmpty())
    }

    /** Excludes wrong origin, method, query, user-info, and path from protected request counting. */
    @Test
    fun ignoresProtectedEndpointMismatches() {
        val collector = BrowserErrorCollector(baseUrl)
        listOf(
            BrowserRequestDetails("POST", "$baseUrl/api/wishlist/getMy"),
            BrowserRequestDetails("GET", "$baseUrl/api/wishlist/getMy?retry=1"),
            BrowserRequestDetails("GET", "$baseUrl/api/wishlist/getMy/extra"),
            BrowserRequestDetails("GET", "http://localhost:32123/api/wishlist/getMy"),
            BrowserRequestDetails("GET", "http://user@127.0.0.1:32123/api/wishlist/getMy"),
            BrowserRequestDetails("GET", "$baseUrl/api/wishlist/getUser"),
        ).forEach(collector::recordRequest)
        assertEquals(0, collector.protectedOwnListRequests())
    }

    /** Keeps response error classification separate from request counting. */
    @Test
    fun responseDoesNotCountAsRequest() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordResponse(BrowserResponseDetails("GET", "$baseUrl/api/wishlist/getMy", 401))
        assertEquals(0, collector.protectedOwnListRequests())
        assertEquals(listOf("Unexpected HTTP 401: GET $baseUrl/api/wishlist/getMy"), collector.unexpectedErrors())
    }

    /** Rejects page exceptions as strict smoke-test failures. */
    @Test
    fun rejectsPageExceptions() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordPageError("Unexpected application error")
        assertEquals(listOf("Unexpected application error"), collector.unexpectedErrors())
    }
}
