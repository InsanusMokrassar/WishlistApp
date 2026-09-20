package dev.inmo.wishlist.browser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Proves that the anonymous bootstrap exception cannot hide authorization failures after registration starts. */
class BrowserResponseClassifierTest {
    /** Generated-server origin used to exercise exact origin matching. */
    private val baseUrl = "http://127.0.0.1:32123"

    /** The single 401 response allowed while a page performs anonymous bootstrap. */
    private val anonymousBootstrap = BrowserResponseDetails("GET", "$baseUrl/api/wishlist/getMy", 401)

    /** Allows only the exact unauthenticated wishlist bootstrap response. */
    @Test
    fun allowsExactAnonymousBootstrap401() {
        assertTrue(isExpectedAnonymousBootstrap401(anonymousBootstrap, BrowserSessionPhase.ANONYMOUS_BOOTSTRAP, baseUrl))
        assertFalse(isExpectedAnonymousBootstrap401(anonymousBootstrap.copy(status = 403), BrowserSessionPhase.ANONYMOUS_BOOTSTRAP, baseUrl))
        assertFalse(isExpectedAnonymousBootstrap401(anonymousBootstrap.copy(method = "POST"), BrowserSessionPhase.ANONYMOUS_BOOTSTRAP, baseUrl))
        assertFalse(isExpectedAnonymousBootstrap401(anonymousBootstrap.copy(url = "$baseUrl/api/wishlist/getMy/extra"), BrowserSessionPhase.ANONYMOUS_BOOTSTRAP, baseUrl))
        assertFalse(isExpectedAnonymousBootstrap401(anonymousBootstrap.copy(url = "http://localhost:32123/api/wishlist/getMy"), BrowserSessionPhase.ANONYMOUS_BOOTSTRAP, baseUrl))
    }

    /** Rejects the same 401 once account creation has started. */
    @Test
    fun rejectsBootstrap401AfterAuthenticationBegins() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.moveTo(BrowserSessionPhase.AUTHENTICATING)
        collector.recordResponse(anonymousBootstrap)
        assertEquals(listOf("Unexpected HTTP 401 during AUTHENTICATING: GET $baseUrl/api/wishlist/getMy"), collector.unexpectedErrors())
    }

    /** Keeps a generic console message narrow by requiring the matching observed anonymous response. */
    @Test
    fun allowsOnlyObservedAnonymousBootstrapConsoleMessage() {
        val collector = BrowserErrorCollector(baseUrl)
        collector.recordConsoleError("Failed to load resource: the server responded with a status of 401", null)
        collector.recordResponse(anonymousBootstrap)
        collector.recordConsoleError("Failed to load resource: the server responded with a status of 401", null)
        collector.recordConsoleError("NoTransformationFoundException", "$baseUrl/api/auth/register")
        assertEquals(
            listOf(
                "NoTransformationFoundException",
            ),
            collector.unexpectedErrors(),
        )
    }
}
