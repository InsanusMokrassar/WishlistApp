package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlin.test.assertTrue

/** Exercises status handling and caller-scoped payloads through the production email client. */
class KtorEmailFeatureTest {
    @Test
    fun enabledDecodesSuccessfulTrueAndFalseBodies() = runTest {
        listOf(true, false).forEach { expected ->
            val client = jsonClient { respondJson(expected.toString()) }
            try {
                assertEquals(expected, KtorEmailFeature(client).isFeatureEnabled())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun capabilityAndVerificationRejectNonSuccessResponses() = runTest {
        val client = jsonClient { respondJson("\"Sent\"", HttpStatusCode.InternalServerError) }
        try {
            assertThrows { KtorEmailFeature(client).isFeatureEnabled() }
            assertThrows { KtorEmailFeature(client).requestMyEmailVerification(Email("owner@example.com")) }
        } finally {
            client.close()
        }
    }

    @Test
    fun verificationPostsOnlyExpectedEmailAndDecodesEveryResult() = runTest {
        EmailVerificationRequestResult.entries.forEach { expected ->
            var requestBody = ""
            val client = jsonClient { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/email/requestMyEmailVerification", request.url.encodedPath)
                requestBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                respondJson("\"${expected.name}\"")
            }
            try {
                assertEquals(
                    expected,
                    KtorEmailFeature(client).requestMyEmailVerification(Email("owner@example.com")),
                )
                assertEquals("{\"expectedEmail\":\"owner@example.com\"}", requestBody)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun capabilityNetworkFailurePropagatesToTheRecoverableViewModelState() = runTest {
        val client = jsonClient { throw IllegalStateException("network unavailable") }
        try {
            assertThrows { KtorEmailFeature(client).isFeatureEnabled() }
        } finally {
            client.close()
        }
    }
}

private fun jsonClient(handler: suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
    HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json() }
    }

private fun MockRequestHandleScope.respondJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)

private suspend fun assertThrows(block: suspend () -> Unit) {
    var thrown = false
    try {
        block()
    } catch (_: Throwable) {
        thrown = true
    }
    assertTrue(thrown)
}
