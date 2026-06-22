package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinksConstants
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.HandleResult
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Registers the user-facing deeplink route `GET {id}` under [DeepLinksConstants.linksPrefixPathPart],
 * resolving to `/api/links/{deeplink_uuid}` once `InternalApplicationRoutingConfigurator` wraps every
 * [ApplicationRoutingConfigurator.Element] under the global `/api` prefix.
 *
 * Status mapping: blank/missing id -> 400; [HandleResult.NotFound]/[HandleResult.Unhandled] -> 404;
 * [HandleResult.Handled] -> 200. A present-but-bogus id is a normal lookup miss (404), since
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
