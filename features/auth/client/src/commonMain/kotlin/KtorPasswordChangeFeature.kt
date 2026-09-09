package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.client.utils.PasswordChangeCompletionUrl
import dev.inmo.wishlist.features.auth.client.utils.skipDefaultServerUrl
import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.email.common.models.Email
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Transport-only realization of [PasswordChangeFeature].
 *
 * Completion is explicitly isolated from bearer refresh and credential storage; a browser value,
 * when installed by JS startup, pins only this request to the origin serving the approval page.
 *
 * @param client Shared configured HTTP client.
 * @param completionUrl Optional JS-owned trusted absolute completion URL.
 */
class KtorPasswordChangeFeature(
    private val client: HttpClient,
    private val completionUrl: PasswordChangeCompletionUrl? = null,
) : PasswordChangeFeature {
    /** Relative route used by ordinary configured-server requests. */
    private val requestPath = "${Constants.prefixPathPart}/${Constants.requestPasswordChangeEmailPathPart}"

    /** Relative route used by native completion when no browser origin binding exists. */
    private val completionPath = "${Constants.prefixPathPart}/${Constants.completePasswordChangePathPart}"

    override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult? =
        decodeSuccessful<PasswordChangeEmailRequestResult> {
            client.post(requestPath) {
                contentType(ContentType.Application.Json)
                setBody(PasswordChangeEmailRequest(expectedEmail))
            }
        }

    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult? =
        decodeSuccessful<PasswordChangeResult> {
            client.post(completionUrl?.string ?: completionPath) {
                attributes.put(AuthCircuitBreaker, Unit)
                contentType(ContentType.Application.Json)
                if (completionUrl != null) {
                    attributes.put(skipDefaultServerUrl, Unit)
                }
                setBody(request)
            }
        }

    /** Decodes only successful typed responses while preserving coroutine cancellation. */
    private suspend inline fun <reified T> decodeSuccessful(request: () -> io.ktor.client.statement.HttpResponse): T? =
        try {
            val response = request()
            if (!response.status.isSuccess()) return null
            response.body<T>()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }
}
