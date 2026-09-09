package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.client.utils.PasswordChangeCompletionUrl
import dev.inmo.wishlist.features.auth.client.utils.skipDefaultServerUrl
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies password-change transport keeps request identities exact and failures fail closed. */
class KtorPasswordChangeFeatureTest {
    /** An authenticated request carries only the expected current email and decodes typed outcomes. */
    @Test
    fun requestPostsExpectedEmailAndDecodesSuccessfulOutcome() = runTest {
        var body = ""
        var observedPath = ""
        var observedMethod: HttpMethod? = null
        val client = passwordChangeJsonClient { request ->
            observedMethod = request.method
            observedPath = request.url.encodedPath
            body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondPasswordChangeJson("\"Sent\"")
        }
        try {
            assertEquals(
                PasswordChangeEmailRequestResult.Sent,
                KtorPasswordChangeFeature(client).requestPasswordChangeEmail(Email("owner@example.com")),
                "method=$observedMethod path=$observedPath body=$body",
            )
            assertEquals(HttpMethod.Post, observedMethod)
            assertEquals("/auth/requestPasswordChangeEmail", observedPath)
            assertEquals("{\"expectedEmail\":\"owner@example.com\"}", body)
        } finally {
            client.close()
        }
    }

    /** Browser completion uses its issuing origin, skips bearer refresh, and preserves the UUID verbatim. */
    @Test
    fun completionUsesIssuingOriginAndExactApprovalBody() = runTest {
        var body = ""
        var observedUrl = ""
        var hasCircuitBreaker = false
        var hasUrlOptOut = false
        val client = passwordChangeJsonClient { request ->
            observedUrl = request.url.toString()
            hasCircuitBreaker = request.attributes.contains(AuthCircuitBreaker)
            hasUrlOptOut = request.attributes.contains(skipDefaultServerUrl)
            body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondPasswordChangeJson("\"Changed\"")
        }
        val request = CompletePasswordChangeRequest(
            userId = UserId(7L),
            approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
            password = Password("new-password"),
        )
        try {
            assertEquals(
                PasswordChangeResult.Changed,
                KtorPasswordChangeFeature(
                    client,
                    PasswordChangeCompletionUrl("https://approval.example/api/auth/completePasswordChange"),
                ).completePasswordChange(request),
                "url=$observedUrl circuitBreaker=$hasCircuitBreaker urlOptOut=$hasUrlOptOut body=$body",
            )
            assertTrue(observedUrl.startsWith("https://approval.example/api/auth/completePasswordChange"))
            assertTrue(hasCircuitBreaker)
            assertTrue(hasUrlOptOut)
            assertEquals(
                "{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}",
                body,
            )
        } finally {
            client.close()
        }
    }

    /** Non-success statuses, malformed JSON, and unknown result values cannot claim a changed password. */
    @Test
    fun completionFailuresReturnNoOutcome() = runTest {
        listOf(
            HttpStatusCode.BadRequest to "\"Changed\"",
            HttpStatusCode.OK to "not-json",
            HttpStatusCode.OK to "\"FutureResult\"",
        ).forEach { (status, content) ->
            val client = passwordChangeJsonClient { respondPasswordChangeJson(content, status) }
            try {
                assertNull(KtorPasswordChangeFeature(client).completePasswordChange(validRequest()))
            } finally {
                client.close()
            }
        }
    }

    /** Builds the one valid representative completion DTO used by status-failure tests. */
    private fun validRequest(): CompletePasswordChangeRequest = CompletePasswordChangeRequest(
        userId = UserId(7L),
        approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
        password = Password("new-password"),
    )
}

/** Builds a MockEngine client with the production JSON codec used by password-change transport. */
private fun passwordChangeJsonClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): HttpClient = HttpClient(MockEngine(handler)) {
    install(ContentNegotiation) { json() }
}

/** Returns a JSON response with the content type required by Ktor's body decoder. */
private fun MockRequestHandleScope.respondPasswordChangeJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)
