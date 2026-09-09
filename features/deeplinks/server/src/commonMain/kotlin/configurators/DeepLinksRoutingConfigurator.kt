package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinksConstants
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException

/**
 * Registers the user-facing deeplink route `GET {id}` under [DeepLinksConstants.linksPrefixPathPart],
 * resolving to `/api/links/{deeplink_uuid}` once `InternalApplicationRoutingConfigurator` wraps every
 * [ApplicationRoutingConfigurator.Element] under the global `/api` prefix.
 *
 * Status mapping: blank/missing id -> 400; [HandleResult.NotFound]/[HandleResult.Unhandled] -> 404;
 * [HandleResult.Handled.Common] -> 200; [HandleResult.Handled.Redirect] -> 302. A present-but-bogus id is a normal lookup miss (404), since
 * [DeepLinkId] is an opaque string and UUID format is not validated.
 *
 * @param service Server-only service that resolves an opened deeplink to a [HandleResult].
 */
class DeepLinksRoutingConfigurator(
    private val service: DeepLinksService
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(DeepLinksConstants.linksPrefixPathPart) {
            get("{${DeepLinksConstants.deeplinkIdParameter}}") {
                call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                call.response.headers.append("Referrer-Policy", "no-referrer")
                val deeplinkId = call.parameters[DeepLinksConstants.deeplinkIdParameter]
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::DeepLinkId)
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                try {
                    when (val result = service.handle(deeplinkId)) {
                        HandleResult.Handled.Common -> call.respond(HttpStatusCode.OK)
                        is HandleResult.Handled.Redirect -> call.respondRedirect(result.url, permanent = false)
                        HandleResult.NotFound,
                        HandleResult.Unhandled -> call.respond(HttpStatusCode.NotFound)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
