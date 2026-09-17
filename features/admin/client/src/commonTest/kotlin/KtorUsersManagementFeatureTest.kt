package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises the username-only admin mutation through the production Ktor client. */
class KtorUsersManagementFeatureTest {
    @Test
    fun updateUsernameUsesItsDedicatedPathAndSerializedUsername() = runTest {
        var requestBody = ""
        val client = jsonClient { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/admin/users/setUsername/17", request.url.encodedPath)
            requestBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondJson("", HttpStatusCode.OK)
        }
        try {
            assertTrue(KtorUsersManagementFeature(client).updateUsername(UserId(17L), Username("renamed")))
            assertEquals("\"renamed\"", requestBody)
        } finally {
            client.close()
        }
    }

    @Test
    fun updateUsernameReturnsFalseForAllNormalNonSuccessStatuses() = runTest {
        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.NotFound,
            HttpStatusCode.Conflict,
            HttpStatusCode.InternalServerError,
        ).forEach { status ->
            val client = jsonClient { respondJson("", status) }
            try {
                assertFalse(KtorUsersManagementFeature(client).updateUsername(UserId(17L), Username("renamed")))
            } finally {
                client.close()
            }
        }
    }
}

private fun jsonClient(handler: suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
    HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json() }
        defaultRequest { contentType(ContentType.Application.Json) }
    }

private fun MockRequestHandleScope.respondJson(
    content: String,
    status: HttpStatusCode,
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)
