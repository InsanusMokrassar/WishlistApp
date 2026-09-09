package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies deeplink responses apply redaction headers before any purpose-specific handling. */
class DeepLinksRoutingConfiguratorTest {
    /** Redirects and lookup failures both suppress caching and referrer disclosure. */
    @Test
    fun everyResolutionOutcomeCarriesNoStoreAndNoReferrer() = testApplication {
        val repo = RoutingDeepLinksRepo()
        val handlerId = DeepLinkHandlerId("redirect")
        val approvalId = DeepLinkId("approval-id")
        repo.set(approvalId, DeepLinkHandlerInfo(handlerId, "value"))
        val service = DeepLinksService(
            repo,
            listOf(
                object : DeepLinkHandler {
                    override val id: DeepLinkHandlerId = handlerId

                    override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled =
                        HandleResult.Handled.Redirect("/password-change/7/approval-id")
                },
            ),
        )
        application {
            routing {
                route("/api") {
                    with(DeepLinksRoutingConfigurator(service)) { invoke() }
                }
            }
        }
        val client = createClient { followRedirects = false }

        listOf(
            client.get("/api/links/${approvalId.string}") to HttpStatusCode.Found,
            client.get("/api/links/missing") to HttpStatusCode.NotFound,
        ).forEach { (response, expectedStatus) ->
            assertEquals(expectedStatus, response.status)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
        }
    }

    /** Converts internal lookup or handler failures to a policy-protected public 500 response. */
    @Test
    fun handlerFailureIsSanitizedToInternalServerError() = testApplication {
        val repo = RoutingDeepLinksRepo()
        val handlerId = DeepLinkHandlerId("failing")
        val approvalId = DeepLinkId("approval-id")
        repo.set(approvalId, DeepLinkHandlerInfo(handlerId, "value"))
        val service = DeepLinksService(repo, listOf(object : DeepLinkHandler {
            override val id = handlerId
            override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): HandleResult.Handled? = error("storage detail")
        }))
        application { routing { route("/api") { with(DeepLinksRoutingConfigurator(service)) { invoke() } } } }
        val response = createClient { followRedirects = false }.get("/api/links/${approvalId.string}")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
    }

    /** Minimal persistent repository for the actual routing/service boundary. */
    private class RoutingDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()
}
