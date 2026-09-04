package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies configuration transport compatibility with servers predating `/auth/config`. */
class KtorAuthFeatureTest {
    /** A missing config route falls back to the legacy availability endpoint in request order. */
    @Test
    fun missingConfigRouteFallsBackToLegacyAvailability() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/auth/config" -> respond("", HttpStatusCode.NotFound)
                "/auth/is_registration_available" -> respondJson("true")
                else -> error("Unexpected request: ${request.url}")
            }
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = KtorAuthFeature(client).getConfig()

        assertEquals(AuthConfig(enableRegistration = true), result)
        assertEquals(listOf("/auth/config", "/auth/is_registration_available"), paths)
        client.close()
    }

    /** A config decode failure falls back and preserves the legacy fail-closed value. */
    @Test
    fun invalidConfigBodyFallsBackToLegacyAvailability() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/auth/config" -> respondJson("not-json")
                "/auth/is_registration_available" -> respondJson("false")
                else -> error("Unexpected request: ${request.url}")
            }
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = KtorAuthFeature(client).getConfig()

        assertEquals(AuthConfig(), result)
        assertEquals(listOf("/auth/config", "/auth/is_registration_available"), paths)
        client.close()
    }

    /** A successful modern response is returned unchanged without probing the legacy route. */
    @Test
    fun successfulConfigResponseDoesNotCallLegacyRoute() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respondJson("{\"enableRegistration\":true,\"requireEmailForRegistration\":true}")
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = KtorAuthFeature(client).getConfig()

        assertEquals(AuthConfig(true, true), result)
        assertEquals(listOf("/auth/config"), paths)
        client.close()
    }

    /** Failure of both modern and legacy probes returns the disabled optional-email default. */
    @Test
    fun bothConfigurationProbesFailClosed() = runTest {
        val client = HttpClient(MockEngine {
            respond("", HttpStatusCode.InternalServerError)
        }) {
            install(ContentNegotiation) { json() }
        }

        assertEquals(AuthConfig(), KtorAuthFeature(client).getConfig())
        client.close()
    }
}

/** Builds a JSON mock response with the content type required by Ktor deserialization. */
private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(content: String) =
    respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
