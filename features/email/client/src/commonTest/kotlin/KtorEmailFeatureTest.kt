package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.email.common.models.EmailChangeCooldownException
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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/** Exercises status handling and caller-scoped payloads through the production email client. */
class KtorEmailFeatureTest {
    @Test
    fun getMyEmailGetsExistingPopulatedAndEmptyProfiles() = runTest {
        listOf(
            EmailProfile(
                userId = 7L,
                email = Email("approved@example.com"),
                emailApproved = true,
                pendingEmail = Email("pending@example.com"),
                emailChangeRequestedAt = 101L,
                emailChangeAllowedAt = 202L,
            ),
            EmailProfile(userId = 8L),
        ).forEach { expected ->
            val client = jsonClient { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/email/myEmail", request.url.encodedPath)
                assertTrue(request.url.parameters.isEmpty())
                respondJson(Json.encodeToString(EmailProfile.serializer(), expected))
            }
            try {
                assertEquals(expected, KtorEmailFeature(client).getMyEmail())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun getMyEmailMapsOnlyNotFoundToNull() = runTest {
        val missingClient = jsonClient { respondJson("{}", HttpStatusCode.NotFound) }
        try {
            assertEquals(null, KtorEmailFeature(missingClient).getMyEmail())
        } finally {
            missingClient.close()
        }

        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.InternalServerError,
        ).forEach { status ->
            val failureClient = jsonClient {
                respondJson("""{"userId":7,"email":"owner@example.com"}""", status)
            }
            try {
                assertThrows { KtorEmailFeature(failureClient).getMyEmail() }
            } finally {
                failureClient.close()
            }
        }
    }

    @Test
    fun getMyEmailRejectsMalformedSuccessfulModelsAndTransportFailures() = runTest {
        listOf(
            "{}",
            "null",
            """{"userId":7,"email":"not-an-email"}""",
            "not-json",
        ).forEach { body ->
            val malformedClient = jsonClient { respondJson(body) }
            try {
                assertThrows { KtorEmailFeature(malformedClient).getMyEmail() }
            } finally {
                malformedClient.close()
            }
        }

        val networkClient = jsonClient { throw IllegalStateException("network unavailable") }
        try {
            assertThrows { KtorEmailFeature(networkClient).getMyEmail() }
        } finally {
            networkClient.close()
        }
    }

    @Test
    fun getMyEmailPropagatesCancellation() = runTest {
        val client = jsonClient { throw CancellationException("cancelled") }
        try {
            assertFailsWith<CancellationException> { KtorEmailFeature(client).getMyEmail() }
        } finally {
            client.close()
        }
    }

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
    fun setMyEmailPutsOnlyEmailAndReturnsStatusSuccess() = runTest {
        val replacement = Email("replacement@example.com")
        listOf(
            HttpStatusCode.OK to true,
            HttpStatusCode.NoContent to true,
            HttpStatusCode.Conflict to false,
            HttpStatusCode.InternalServerError to false,
        ).forEach { (status, expected) ->
            var requestCount = 0
            val client = jsonClient { request ->
                requestCount += 1
                assertEquals(HttpMethod.Put, request.method)
                assertEquals("/email/myEmail", request.url.encodedPath)
                assertEquals(ContentType.Application.Json, request.body.contentType)
                val requestBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                val requestObject = Json.parseToJsonElement(requestBody).jsonObject
                assertEquals(setOf("email"), requestObject.keys)
                assertEquals(JsonPrimitive(replacement.string), requestObject["email"])
                respondJson("", status)
            }
            try {
                assertEquals(expected, KtorEmailFeature(client).setMyEmail(replacement))
                assertEquals(1, requestCount)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun setMyEmailNetworkFailurePropagates() = runTest {
        val client = jsonClient { throw IllegalStateException("network unavailable") }
        try {
            assertThrows { KtorEmailFeature(client).setMyEmail(Email("replacement@example.com")) }
        } finally {
            client.close()
        }
    }

    @Test
    fun setMyEmailDecodesOnlyWellFormedCooldownResponses() = runTest {
        val client = jsonClient {
            respondJson("""{"emailChangeAllowedAt":123456789}""", HttpStatusCode.TooManyRequests)
        }
        try {
            val failure = assertFailsWith<EmailChangeCooldownException> {
                KtorEmailFeature(client).setMyEmail(Email("replacement@example.com"))
            }
            assertEquals(123456789L, failure.cooldown.emailChangeAllowedAt)
        } finally {
            client.close()
        }

        listOf("", "{}", "not-json").forEach { body ->
            val malformedClient = jsonClient { respondJson(body, HttpStatusCode.TooManyRequests) }
            try {
                val failure = assertFailsWith<Throwable> {
                    KtorEmailFeature(malformedClient).setMyEmail(Email("replacement@example.com"))
                }
                assertFalse(failure is EmailChangeCooldownException)
            } finally {
                malformedClient.close()
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
