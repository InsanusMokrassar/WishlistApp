package dev.inmo.wishlist.features.email.server.configurators

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.email.server.services.FakeEmailsService
import dev.inmo.wishlist.features.email.server.services.FakeRolesFeature
import dev.inmo.wishlist.features.email.server.services.FakeRolesRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.CacheUsersRepo
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import io.ktor.client.request.header
import io.ktor.client.request.get
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies caller extraction and request-result mapping through the installed email routes. */
class EmailRoutingsConfiguratorTest {
    /** A bearer GET bypasses a warmed user cache and serializes the independent fresh email projection. */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getMyEmailReadsFreshProfileThroughWarmedCacheAfterIndependentWrite() = runTest {
        val file = Files.createTempFile("wishlist-email-route", ".sqlite")
        val database = Database.connect(url = "jdbc:sqlite:${file.toAbsolutePath()}", driver = "org.sqlite.JDBC")
        try {
            var now = 1_000L
            val cachedBacking = ExposedUsersRepo(database, nowMillis = { now })
            val approved = Email("approved@example.com")
            val replacement = Email("replacement@example.com")
            val created = cachedBacking.create(listOf(NewUser(Username("owner"), approved))).single()
            val cache = CacheUsersRepo(cachedBacking, backgroundScope)
            advanceUntilIdle()
            assertEquals(created, cache.getById(created.id))

            val independent = ExposedUsersRepo(database, nowMillis = { now })
            assertTrue(independent.approveEmail(created.id, approved) != null)
            now = 1_010L
            assertTrue(independent.update(created.id, NewUser(created.username, replacement)) != null)
            assertEquals(created, cache.getById(created.id))

            val feature = EmailFeatureService(
                emailsService = FakeEmailsService(),
                accountCoordinator = EmailVerificationAccountCoordinator(cache, FakeRolesRepo()),
                rolesFeature = FakeRolesFeature(),
            )
            testApplication {
                installEmailRoutes(feature)

                val response = client.get("/api/email/myEmail") {
                    header(HttpHeaders.Authorization, "Bearer owner")
                }

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    EmailProfile(
                        userId = created.id.long,
                        email = approved,
                        emailApproved = true,
                        pendingEmail = replacement,
                        emailChangeRequestedAt = 1_010L,
                    ),
                    Json.decodeFromString(EmailProfile.serializer(), response.bodyAsText()),
                )
            }
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                Files.deleteIfExists(file)
            }
        }
    }

    @Test
    fun getMyEmailRejectsMissingAndInvalidBearerBeforeCallingFeature() = testApplication {
        val feature = RecordingEmailFeature()
        installEmailRoutes(feature)

        listOf(null, "Bearer invalid").forEach { authorization ->
            val response = client.get("/api/email/myEmail") {
                authorization?.let { header(HttpHeaders.Authorization, it) }
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        assertTrue(feature.myEmailCalls.isEmpty())
    }

    @Test
    fun getMyEmailReturnsOnlyBearerOwnersPopulatedFreshProfile() = testApplication {
        val ownerProfile = EmailProfile(
            userId = 1L,
            email = Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeRequestedAt = 101L,
            emailChangeAllowedAt = 202L,
        )
        val otherProfile = EmailProfile(userId = 2L, email = Email("other@example.com"))
        val feature = RecordingEmailFeature(
            myEmailProfiles = mapOf(UserId(1L) to ownerProfile, UserId(2L) to otherProfile),
        )
        installEmailRoutes(feature)

        val response = client.get("/api/email/myEmail?userId=2&callerId=2&rootId=2") {
            header(HttpHeaders.Authorization, "Bearer owner")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            ownerProfile,
            Json.decodeFromString(EmailProfile.serializer(), response.bodyAsText()),
        )
        assertEquals(listOf(UserId(1L)), feature.myEmailCalls)
        assertFalse(response.bodyAsText().contains("other@example.com"))
    }

    @Test
    fun getMyEmailReturnsEmptyProfileForExistingOwnerAndNotFoundForMissingOwner() = testApplication {
        val emptyProfile = EmailProfile(userId = 1L)
        val feature = RecordingEmailFeature(myEmailProfiles = mapOf(UserId(1L) to emptyProfile))
        installEmailRoutes(feature)

        val populated = client.get("/api/email/myEmail") {
            header(HttpHeaders.Authorization, "Bearer owner")
        }
        val missing = client.get("/api/email/myEmail") {
            header(HttpHeaders.Authorization, "Bearer other")
        }

        assertEquals(HttpStatusCode.OK, populated.status)
        assertEquals(
            emptyProfile,
            Json.decodeFromString(EmailProfile.serializer(), populated.bodyAsText()),
        )
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(listOf(UserId(1L), UserId(2L)), feature.myEmailCalls)
    }

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
        private val myEmailProfiles: Map<UserId, EmailProfile> = emptyMap(),
    ) : EmailFeature {
        val myEmailCalls = mutableListOf<UserId>()
        val setEmailCalls = mutableListOf<Pair<UserId, Email?>>()
        val verificationCalls = mutableListOf<Pair<UserId, Email>>()

        override suspend fun isFeatureEnabled(): Boolean = true

        override suspend fun getMyEmail(callerId: UserId): EmailProfile? {
            myEmailCalls += callerId
            return myEmailProfiles[callerId]
        }

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
