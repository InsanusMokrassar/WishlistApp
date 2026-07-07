package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.SetEmailRequest
import dev.inmo.wishlist.features.email.common.models.TestEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * HTTP-only [dev.inmo.wishlist.features.email.client.EmailFeature] implementation that forwards
 * requests to the server over the shared [HttpClient].
 *
 * Per the project's Ktor realization rule, this class performs no caching, state management, or
 * business logic — it only translates method calls into HTTP requests and returns the result.
 *
 * @param client Shared HTTP client already configured with auth, serialization, and base URL.
 */
class KtorEmailFeature(private val client: HttpClient) : EmailFeature {

    /** Path for `GET /email/enabled`. */
    private val enabledPath =
        "${EmailConstants.prefixPathPart}/${EmailConstants.enabledPathPart}"

    /** Path for `POST /email/sendTest`. */
    private val sendTestPath =
        "${EmailConstants.prefixPathPart}/${EmailConstants.sendTestPathPart}"

    /** Path for `PUT /email/myEmail`. */
    private val myEmailPath =
        "${EmailConstants.prefixPathPart}/${EmailConstants.myEmailPathPart}"

    /**
     * Checks whether the server-side email feature is enabled.
     *
     * @return Server-reported enabled flag, or `false` on failure.
     */
    override suspend fun isFeatureEnabled(): Boolean {
        val response = client.get(enabledPath)
        return if (response.status.isSuccess()) response.body() else false
    }

    /**
     * Requests a test email delivery to [recipient] from the server.
     *
     * @param recipient Target address for the test message.
     * @return `true` when the server accepted and delivered the message; `false` on failure.
     */
    override suspend fun sendTestEmail(recipient: Email): Boolean {
        val response = client.post(sendTestPath) {
            contentType(ContentType.Application.Json)
            setBody(TestEmailRequest(recipient))
        }
        return response.status.isSuccess()
    }

    /**
     * Stores or clears the authenticated caller's own email address on the server.
     *
     * @param email New address to persist, or `null` to clear.
     * @return `true` when the update was accepted; `false` on failure.
     */
    override suspend fun setMyEmail(email: Email?): Boolean {
        val response = client.put(myEmailPath) {
            contentType(ContentType.Application.Json)
            setBody(SetEmailRequest(email))
        }
        return response.status.isSuccess()
    }
}
