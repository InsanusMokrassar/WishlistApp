package dev.inmo.wishlist.browser

import java.net.URI

/** A browser request retained for exact anonymous protected-endpoint assertions. */
internal data class BrowserRequestDetails(
    /** HTTP method sent by the browser. */
    val method: String,
    /** Absolute request URL reported by Playwright. */
    val url: String,
)

/** A browser response retained for precise smoke-test diagnostics. */
internal data class BrowserResponseDetails(
    /** HTTP method sent by the browser. */
    val method: String,
    /** Absolute response URL reported by Playwright. */
    val url: String,
    /** HTTP status code reported by Playwright. */
    val status: Int,
)

/** Collects every browser error and counts calls to the protected own-list endpoint. */
internal class BrowserErrorCollector(
    /** Same-origin server URL generated for the current browser suite. */
    private val baseUrl: String,
) {
    /** Response and page errors that must fail the smoke test. */
    private val errors = mutableListOf<String>()

    /** Number of same-origin requests made to the protected caller-owned wishlist endpoint. */
    private var protectedOwnListRequestCount = 0

    /** Records a request so attempts that fail before a response remain visible to the smoke assertion. */
    fun recordRequest(request: BrowserRequestDetails) {
        if (isProtectedOwnListRequest(request.method, request.url)) protectedOwnListRequestCount += 1
    }

    /** Records a response and rejects every HTTP 401. */
    fun recordResponse(response: BrowserResponseDetails) {
        if (response.status == 401) errors += "Unexpected HTTP 401: ${response.method} ${response.url}"
    }

    /** Records every browser console error without suppression. */
    fun recordConsoleError(text: String) {
        errors += text
    }

    /** Records a page exception, which is never an expected bootstrap condition. */
    fun recordPageError(message: String) {
        errors += message
    }

    /** Returns a stable snapshot for assertion after page interaction has finished. */
    fun unexpectedErrors(): List<String> = errors.toList()

    /** Returns the number of exact same-origin protected caller-list requests observed so far. */
    fun protectedOwnListRequests(): Int = protectedOwnListRequestCount

    /** Matches only the exact same-origin protected caller-list request. */
    private fun isProtectedOwnListRequest(method: String, url: String): Boolean = runCatching {
        val base = URI(baseUrl)
        val target = URI(url).normalize()
        method.equals("GET", ignoreCase = true) &&
            target.scheme == base.scheme && target.host == base.host && target.port == base.port &&
            target.userInfo == null && target.rawQuery == null && target.path == "/api/wishlist/getMy"
    }.getOrDefault(false)
}
