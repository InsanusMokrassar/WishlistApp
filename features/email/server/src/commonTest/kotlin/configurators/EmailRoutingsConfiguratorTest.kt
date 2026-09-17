package dev.inmo.wishlist.features.email.server.configurators

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies caller extraction and request-result mapping through the installed email routes. */
class EmailRoutingsConfiguratorTest {
    @Test
    fun setEmailRejectsMissingAndInvalidBearerBeforeCallingFeature() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        listOf(null, "Bearer invalid").forEach { authorization ->
            val response = client.put("/api/email/myEmail") {
                authorization?.let { header(HttpHeaders.Authorization, it) }
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"replacement@example.com\"}")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        assertEquals(emptyList(), feature.setEmailCalls)
    }

    @Test
    fun setEmailUsesBearerCallerDespiteForgedAccountSelectors() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        listOf("owner" to UserId(1L), "other" to UserId(2L)).forEach { (token, callerId) ->
            val response = client.put("/api/email/myEmail") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"email":"replacement@example.com","callerId":999,"userId":998,"selectedUserId":997,"rootId":996}"""
                )
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val expectedCall: Pair<UserId, Email?> = callerId to Email("replacement@example.com")
            assertEquals(expectedCall, feature.setEmailCalls.last())
        }

        val expectedCalls: List<Pair<UserId, Email?>> =
            listOf(
                UserId(1L) to Email("replacement@example.com"),
                UserId(2L) to Email("replacement@example.com"),
            )
        assertEquals(expectedCalls, feature.setEmailCalls)
    }

    @Test
    fun setEmailMapsFeatureBooleanOutcomeToExistingStatuses() {
        listOf(true to HttpStatusCode.OK, false to HttpStatusCode.InternalServerError).forEach { (updated, status) ->
            testApplication {
                val feature = RecordingEmailFeature(setEmailResult = updated)
                installEmailRoutes(feature)

                val response = client.put("/api/email/myEmail") {
                    header(HttpHeaders.Authorization, "Bearer owner")
                    contentType(ContentType.Application.Json)
                    setBody("{\"email\":\"replacement@example.com\"}")
                }

                assertEquals(status, response.status)
                val expectedCalls: List<Pair<UserId, Email?>> =
                    listOf(UserId(1L) to Email("replacement@example.com"))
                assertEquals(expectedCalls, feature.setEmailCalls)
            }
        }
    }

    @Test
    fun setEmailMapsDuplicateToConflict() = testApplication {
        val feature = RecordingEmailFeature(setEmailFailure = DuplicateUserFieldException())
        installEmailRoutes(feature)

        val response = client.put("/api/email/myEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{\"email\":\"replacement@example.com\"}")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val expectedCalls: List<Pair<UserId, Email?>> =
            listOf(UserId(1L) to Email("replacement@example.com"))
        assertEquals(expectedCalls, feature.setEmailCalls)
    }

    @Test
    fun setEmailMapsCooldownToTypedDeadlineBody() = testApplication {
        val feature = RecordingEmailFeature(setEmailFailure = EmailChangeCooldownException(123456789L))
        installEmailRoutes(feature)

        val response = client.put("/api/email/myEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{\"email\":\"replacement@example.com\"}")
        }

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("{\"emailChangeAllowedAt\":123456789}", response.bodyAsText())
        val expectedCalls: List<Pair<UserId, Email?>> = listOf(UserId(1L) to Email("replacement@example.com"))
        assertEquals(expectedCalls, feature.setEmailCalls)
    }

    @Test
    fun setEmailRejectsMalformedOrInvalidBodyBeforeCallingFeature() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        listOf("{", "{\"email\":\"not-an-email\"}").forEach { body ->
            val response = client.put("/api/email/myEmail") {
                header(HttpHeaders.Authorization, "Bearer owner")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
        assertEquals(emptyList(), feature.setEmailCalls)
    }

    @Test
    fun verificationRouteRejectsMissingOrInvalidBearerBeforeCallingFeature() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        listOf(null, "Bearer invalid").forEach { authorization ->
            val response = client.post("/api/email/requestMyEmailVerification") {
                authorization?.let { header(HttpHeaders.Authorization, it) }
                contentType(ContentType.Application.Json)
                setBody("{\"expectedEmail\":\"owner@example.com\"}")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        assertEquals(emptyList(), feature.verificationCalls)
    }

    @Test
    fun verificationRouteUsesAuthenticatedCallerAndReturnsEveryDomainResult() {
        EmailVerificationRequestResult.entries.forEach { expected ->
            testApplication {
                val feature = RecordingEmailFeature(verificationResult = expected)
                installEmailRoutes(feature)

                val response = client.post("/api/email/requestMyEmailVerification") {
                    header(HttpHeaders.Authorization, "Bearer owner")
                    contentType(ContentType.Application.Json)
                    setBody("{\"expectedEmail\":\"owner@example.com\",\"callerId\":999}")
                }

                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(response.bodyAsText().contains(expected.name))
                assertEquals(listOf(UserId(1L) to Email("owner@example.com")), feature.verificationCalls)
            }
        }
    }

    @Test
    fun verificationRouteReturnsBadRequestForMalformedBodyWithoutCallingFeature() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        val response = client.post("/api/email/requestMyEmailVerification") {
            header(HttpHeaders.Authorization, "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("{")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(emptyList(), feature.verificationCalls)
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.installEmailRoutes(feature: EmailFeature) {
        application {
            install(Authentication) {
                bearer {
                    authenticate { credential ->
                        when (credential.token) {
                            "owner" -> UserId(1L)
                            "other" -> UserId(2L)
                            else -> null
                        }
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                route("/api") {
                    with(EmailRoutingsConfigurator(feature)) { invoke() }
                }
            }
        }
    }

    private class RecordingEmailFeature(
        private val verificationResult: EmailVerificationRequestResult = EmailVerificationRequestResult.Sent,
        private val setEmailResult: Boolean = true,
        private val setEmailFailure: Throwable? = null,
    ) : EmailFeature {
        val setEmailCalls = mutableListOf<Pair<UserId, Email?>>()
        val verificationCalls = mutableListOf<Pair<UserId, Email>>()

        override suspend fun isFeatureEnabled(): Boolean = true

        override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean = true

        override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean {
            setEmailCalls += callerId to email
            setEmailFailure?.let { throw it }
            return setEmailResult
        }

        override suspend fun requestMyEmailVerification(
            callerId: UserId,
            expectedEmail: Email,
        ): EmailVerificationRequestResult {
            verificationCalls += callerId to expectedEmail
            return verificationResult
        }
    }
}
