package dev.inmo.wishlist.features.deeplinks.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.common.DeepLinksConstants
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.DeepLinksFeature
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Registers the public deeplink resolution endpoint under [DeepLinksConstants.linksPrefixPathPart].
 *
 * - `GET links/{deeplinkId}` → `200 OK` when a handler processed the deeplink, `404 Not Found` when
 *   the deeplink is unknown or no handler recognized its handler-info, `400 Bad Request` on a
 *   missing/blank id.
 *
 * The route is intentionally public: the deeplink UUID itself is the capability token.
 *
 * @param feature Deeplinks capability the route delegates resolution to.
 */
class DeepLinksRoutingsConfigurator(
    private val feature: DeepLinksFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(DeepLinksConstants.linksPrefixPathPart) {
            get("{deeplinkId}") {
                val id = call.parameters["deeplinkId"]?.takeIf { it.isNotBlank() }?.let(::DeepLinkId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                if (feature.resolveDeepLink(id)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
