package dev.inmo.wishlist.features.auth.server.configurators

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies Auth route authorization, anonymous completion, and secret-safe response hardening. */
class PasswordChangeRoutingsConfiguratorTest {
    /** Request issuance requires a valid bearer and forwards only the authenticated caller identity. */
    @Test
    fun requestRouteRequiresBearerAndReturnsTypedDomainResult() = testApplication {
        val feature = RecordingPasswordChangeFeature()
        installPasswordChangeRoutes(feature)

        val unauthenticated = client.post("/api/auth/requestPasswordChangeEmail") {
            contentType(ContentType.Application.Json)
            setBody("{\"expectedEmail\":\"owner@example.com\"}")
        }
        assertEquals(HttpStatusCode.Unauthorized, unauthenticated.status)
        assertTrue(feature.requestCalls.isEmpty())

        val authenticated = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{\"expectedEmail\":\"owner@example.com\",\"callerId\":999}")
        }
        assertEquals(HttpStatusCode.OK, authenticated.status)
        assertEquals("no-store", authenticated.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", authenticated.headers["Referrer-Policy"])
        assertTrue(authenticated.bodyAsText().contains(PasswordChangeEmailRequestResult.Sent.name))
        assertEquals(listOf(UserId(1L) to Email("owner@example.com")), feature.requestCalls)
    }

    /** Completion is anonymous, retains the supplied subject assertion, and emits no-store/no-referrer headers. */
    @Test
    fun completionRouteIsAnonymousAndForwardsExactPayload() = testApplication {
        val feature = RecordingPasswordChangeFeature()
        installPasswordChangeRoutes(feature)
        val approvalId = "123e4567-e89b-42d3-a456-426614174000"

        val response = client.post("/api/auth/completePasswordChange") {
            contentType(ContentType.Application.Json)
            setBody("{\"userId\":7,\"approvalId\":\"$approvalId\",\"password\":\"new-password\"}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
        assertTrue(response.bodyAsText().contains(PasswordChangeResult.Changed.name))
        assertEquals(
            listOf(CompletePasswordChangeRequest(UserId(7L), DeepLinkId(approvalId), Password("new-password"))),
            feature.completionCalls,
        )
    }

    /** Malformed or incomplete route bodies fail before invoking a domain operation. */
    @Test
    fun malformedCompletionBodyFailsClosedWithoutDelegation() = testApplication {
        val feature = RecordingPasswordChangeFeature()
        installPasswordChangeRoutes(feature)
        val malformedRequest = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        assertEquals(HttpStatusCode.BadRequest, malformedRequest.status)
        assertEquals("no-store", malformedRequest.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", malformedRequest.headers["Referrer-Policy"])
        val missingRequestField = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.BadRequest, missingRequestField.status)
        val malformed = client.post("/api/auth/completePasswordChange") {
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        assertEquals(HttpStatusCode.BadRequest, malformed.status)
        assertEquals("no-store", malformed.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", malformed.headers["Referrer-Policy"])
        val missingCompletionField = client.post("/api/auth/completePasswordChange") {
            contentType(ContentType.Application.Json)
            setBody("{\"userId\":7}")
        }
        assertEquals(HttpStatusCode.BadRequest, missingCompletionField.status)
        assertTrue(feature.completionCalls.isEmpty())
        assertTrue(feature.requestCalls.isEmpty())
    }

    /** Missing Email wiring retains Auth startup and returns ordinary fail-closed domain results. */
    @Test
    fun missingEmailBindingReturnsUnavailableWithoutBreakingRoutes() = testApplication {
        installPasswordChangeRoutes(null)
        val missingRequest = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{\"expectedEmail\":\"owner@example.com\"}")
        }
        assertTrue(missingRequest.bodyAsText().contains(PasswordChangeEmailRequestResult.Unavailable.name))
        assertEquals("no-store", missingRequest.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", missingRequest.headers["Referrer-Policy"])
        val missingCompletion = client.post("/api/auth/completePasswordChange") {
            contentType(ContentType.Application.Json)
            setBody("{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}")
        }
        assertTrue(missingCompletion.bodyAsText().contains(PasswordChangeResult.InvalidApproval.name))
        assertEquals("no-store", missingCompletion.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", missingCompletion.headers["Referrer-Policy"])
    }

    /** Routes preserve ordinary feature outcomes without remapping them to exceptional statuses. */
    @Test
    fun requestAndCompletionRoutesReturnEveryDomainOutcomeWithPolicyHeaders() = testApplication {
        val feature = RecordingPasswordChangeFeature()
        installPasswordChangeRoutes(feature)
        PasswordChangeEmailRequestResult.entries.forEach { expected ->
            feature.requestResult = expected
            val response = client.post("/api/auth/requestPasswordChangeEmail") {
                header(HttpHeaders.Authorization, "Bearer owner")
                contentType(ContentType.Application.Json)
                setBody("{\"expectedEmail\":\"owner@example.com\"}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains(expected.name))
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
        }
        PasswordChangeResult.entries.forEach { expected ->
            feature.completionResult = expected
            val response = client.post("/api/auth/completePasswordChange") {
                contentType(ContentType.Application.Json)
                setBody("{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains(expected.name))
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
        }
    }

    /** Route exception boundaries retain cancellation handling while hiding ordinary failure detail. */
    @Test
    fun requestAndCompletionExceptionsAreSanitized() = testApplication {
        val feature = RecordingPasswordChangeFeature()
        installPasswordChangeRoutes(feature)
        val sentinel = "password-change-route-sentinel"
        feature.requestFailure = IllegalStateException(sentinel)
        val requestResponse = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{\"expectedEmail\":\"owner@example.com\"}")
        }
        assertEquals(HttpStatusCode.InternalServerError, requestResponse.status)
        assertFalse(requestResponse.bodyAsText().contains(sentinel))
        assertEquals("no-store", requestResponse.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", requestResponse.headers["Referrer-Policy"])
        feature.requestFailure = null
        feature.completionFailure = IllegalStateException(sentinel)
        val completionResponse = client.post("/api/auth/completePasswordChange") {
            contentType(ContentType.Application.Json)
            setBody("{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}")
        }
        assertEquals(HttpStatusCode.InternalServerError, completionResponse.status)
        assertFalse(completionResponse.bodyAsText().contains(sentinel))
        assertEquals("no-store", completionResponse.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", completionResponse.headers["Referrer-Policy"])
    }

    /** Installs Auth's password routes behind the same bearer identity source used by production. */
    private fun ApplicationTestBuilder.installPasswordChangeRoutes(feature: ServerPasswordChangeFeature?) {
        application {
            install(Authentication) {
                bearer {
                    authenticate { credential -> credential.token.takeIf { it == "owner" }?.let { UserId(1L) } }
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            routing {
                route("/api") {
                    with(PasswordChangeRoutingsConfigurator(feature)) { invoke() }
                }
            }
        }
    }

    /** Records the exact route input while returning ordinary non-secret outcomes. */
    private class RecordingPasswordChangeFeature : ServerPasswordChangeFeature {
        /** Authenticated issuance calls in arrival order. */
        val requestCalls = mutableListOf<Pair<UserId, Email>>()

        /** Anonymous completion DTOs in arrival order. */
        val completionCalls = mutableListOf<CompletePasswordChangeRequest>()

        /** Current domain outcome returned by issuance delegation. */
        var requestResult: PasswordChangeEmailRequestResult = PasswordChangeEmailRequestResult.Sent

        /** Current ordinary failure thrown by issuance delegation. */
        var requestFailure: Throwable? = null

        /** Current domain outcome returned by completion delegation. */
        var completionResult: PasswordChangeResult = PasswordChangeResult.Changed

        /** Current ordinary failure thrown by completion delegation. */
        var completionFailure: Throwable? = null

        /** Records the caller-derived subject and returns a sent result. */
        override suspend fun requestPasswordChangeEmail(
            callerId: UserId,
            expectedEmail: Email,
        ): PasswordChangeEmailRequestResult {
            requestCalls += callerId to expectedEmail
            requestFailure?.let { throw it }
            return requestResult
        }

        /** Records one exact token-authorized DTO and returns a changed result. */
        override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult {
            completionCalls += request
            completionFailure?.let { throw it }
            return completionResult
        }
    }
}
