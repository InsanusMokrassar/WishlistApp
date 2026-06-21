package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinksConstants
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Installs the user-facing deeplink route `GET /links/{deeplink_uuid}` at the SITE ROOT (NOT under
 * `/api`).
 *
 * Implemented as a [KtorApplicationConfigurator] opening its own `routing { }` (like
 * `InternalApplicationRoutingConfigurator`), because every `ApplicationRoutingConfigurator.Element`
 * is force-wrapped under `/api` plus a `/api` 404 catch-all by `InternalApplicationRoutingConfigurator`
 * — so an `Element` could never serve a root `links/...` link. Route specificity makes this explicit
 * route win over the static-SPA `default("index.html")` fallback mounted at root.
 *
 * Status mapping: blank/missing id -> 400; [HandleResult.NotFound]/[HandleResult.Unhandled] -> 404;
 * [HandleResult.Handled] -> 200. A present-but-bogus id is a normal lookup miss (404), since
 * [DeepLinkId] is an opaque string and UUID format is not validated.
 *
 * @param service Server-only service that resolves an opened deeplink to a [HandleResult].
 */
class DeepLinksRoutingConfigurator(
    private val service: DeepLinksService
) : KtorApplicationConfigurator {
    override fun Application.configure() {
        routing {
            route(DeepLinksConstants.linksPrefixPathPart) {
                get("{${DeepLinksConstants.deeplinkIdParameter}}") {
                    val deeplinkId = call.parameters[DeepLinksConstants.deeplinkIdParameter]
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::DeepLinkId)
                        ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                    val status = when (service.handle(deeplinkId)) {
                        HandleResult.Handled -> HttpStatusCode.OK
                        HandleResult.NotFound -> HttpStatusCode.NotFound
                        HandleResult.Unhandled -> HttpStatusCode.NotFound
                    }
                    call.respond(status)
                }
            }
        }
    }
}
