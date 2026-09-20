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

    /** Counts the protected caller-list endpoint independently of its status for the anonymous smoke assertion. */
    @Test
    fun countsProtectedOwnListRequests() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordResponse(BrowserResponseDetails("GET", "$baseUrl/api/wishlist/getMy", 200))
        collector.recordResponse(BrowserResponseDetails("GET", "$baseUrl/api/wishlist/getMy/extra", 200))
        assertEquals(1, collector.protectedOwnListRequests())
        assertTrue(collector.unexpectedErrors().isEmpty())
    }

    /** Rejects page exceptions as strict smoke-test failures. */
    @Test
    fun rejectsPageExceptions() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordPageError("Unexpected application error")
        assertEquals(listOf("Unexpected application error"), collector.unexpectedErrors())
    }
}
