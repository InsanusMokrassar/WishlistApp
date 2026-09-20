package dev.inmo.wishlist.browser

import java.net.URI

/** Tracks the point reached by a page so unauthenticated bootstrap failures cannot mask later failures. */
internal enum class BrowserSessionPhase {
    /** The page may make the single known anonymous wishlist bootstrap request. */
    ANONYMOUS_BOOTSTRAP,

    /** Account creation or login has begun and every 401 is unexpected. */
    AUTHENTICATING,

    /** The authenticated UI is visible and every 401 is unexpected. */
    AUTHENTICATED,
}

/** A browser response retained for precise smoke-test diagnostics. */
internal data class BrowserResponseDetails(
    /** HTTP method sent by the browser. */
    val method: String,
    /** Absolute response URL reported by Playwright. */
    val url: String,
    /** HTTP status code reported by Playwright. */
    val status: Int,
)

/** A console error paired with the phase active when Chromium emitted the message. */
internal data class BrowserConsoleError(
    /** Browser-provided console text. */
    val text: String,
    /** Browser-provided source location when one is available. */
    val sourceUrl: String?,
    /** Page phase active when the console event occurred. */
    val phase: BrowserSessionPhase,
)

/**
 * Identifies the only tolerated anonymous API response in the served application bootstrap.
 *
 * @param response browser response details.
 * @param phase current page phase.
 * @param baseUrl generated same-origin server URL.
 * @return true only for the anonymous `GET /api/wishlist/getMy` 401 response.
 */
internal fun isExpectedAnonymousBootstrap401(
    response: BrowserResponseDetails,
    phase: BrowserSessionPhase,
    baseUrl: String,
): Boolean = runCatching {
    val base = URI(baseUrl)
    val target = URI(response.url).normalize()
    response.method.equals("GET", ignoreCase = true) &&
        response.status == 401 &&
        phase == BrowserSessionPhase.ANONYMOUS_BOOTSTRAP &&
        target.scheme == base.scheme &&
        target.host == base.host &&
        target.port == base.port &&
        target.userInfo == null &&
        target.rawQuery == null &&
        target.path == "/api/wishlist/getMy"
}.getOrDefault(false)

/** Collects unexpected page failures while preserving the observed anonymous bootstrap exception. */
internal class BrowserErrorCollector(
    /** Same-origin server URL generated for the current browser suite. */
    private val baseUrl: String,
) {
    /** Phase held independently for this browser page. */
    private var phase: BrowserSessionPhase = BrowserSessionPhase.ANONYMOUS_BOOTSTRAP

    /** Count of exact anonymous bootstrap responses observed for this page. */
    private var allowedBootstrapResponseCount: Int = 0

    /** Response and page errors that must fail the smoke test. */
    private val errors = mutableListOf<String>()

    /** Console errors deferred until their phase and matching response can be evaluated. */
    private val consoleErrors = mutableListOf<BrowserConsoleError>()

    /** Changes the phase before a request that must not receive anonymous authorization handling. */
    fun moveTo(nextPhase: BrowserSessionPhase) {
        phase = nextPhase
    }

    /** Records a response and rejects every 401 except the exact anonymous bootstrap response. */
    fun recordResponse(response: BrowserResponseDetails) {
        if (response.status != 401) return
        if (isExpectedAnonymousBootstrap401(response, phase, baseUrl)) {
            allowedBootstrapResponseCount += 1
            return
        }
        errors += "Unexpected HTTP 401 during $phase: ${response.method} ${response.url}"
    }

    /** Records a console error for later endpoint-attributed bootstrap classification. */
    fun recordConsoleError(text: String, sourceUrl: String?) {
        consoleErrors += BrowserConsoleError(text, sourceUrl, phase)
    }

    /** Records a page exception, which is never an expected bootstrap condition. */
    fun recordPageError(message: String) {
        errors += message
    }

    /** Returns a stable snapshot for assertion after page interaction has finished. */
    fun unexpectedErrors(): List<String> {
        var remainingBootstrapResponseAttributions = allowedBootstrapResponseCount
        var remainingUnlocatedTransformationAttributions = allowedBootstrapResponseCount
        val unexpectedConsoleErrors = consoleErrors.filterNot { error ->
            val sourceUrl = error.sourceUrl
                ?.replace(Regex(":\\d+:\\d+$"), "")
                ?.takeUnless { it.isBlank() || (!it.startsWith("http://") && !it.startsWith("https://")) }
            val isAttributedBootstrapSource = sourceUrl != null && isExpectedAnonymousBootstrap401(
                BrowserResponseDetails(method = "GET", url = sourceUrl, status = 401),
                error.phase,
                baseUrl,
            )
            when {
                isGenericBootstrap401Message(error.text) && isAttributedBootstrapSource && remainingBootstrapResponseAttributions > 0 -> {
                    remainingBootstrapResponseAttributions -= 1
                    true
                }
                isTransformationBootstrapMessage(error.text) && sourceUrl == null && remainingUnlocatedTransformationAttributions > 0 -> {
                    remainingUnlocatedTransformationAttributions -= 1
                    true
                }
                isTransformationBootstrapMessage(error.text) && isAttributedBootstrapSource && remainingBootstrapResponseAttributions > 0 -> {
                    remainingBootstrapResponseAttributions -= 1
                    true
                }
                else -> false
            }
        }
            .map { it.text }
        if (unexpectedConsoleErrors.isEmpty()) return errors.toList()
        return errors + unexpectedConsoleErrors
    }

    /** Identifies Chromium's generic resource-load message for an unauthorized response. */
    private fun isGenericBootstrap401Message(text: String): Boolean = text.contains("server responded with a status of 401")

    /** Identifies the client transformation error emitted after the known anonymous bootstrap response. */
    private fun isTransformationBootstrapMessage(text: String): Boolean = text.contains("NoTransformationFoundException")
}
