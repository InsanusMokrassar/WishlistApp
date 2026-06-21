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
 * Registers the deep link resolution route as an [ApplicationRoutingConfigurator.Element].
 *
 * Final server path (via [dev.inmo.wishlist.features.common.server.configurators.InternalApplicationRoutingConfigurator]):
 * `GET /api/links/{deeplinkId}`.
 *
 * No authentication required — the UUID itself is a capability token. Response codes:
 * - 400 Bad Request: `deeplinkId` parameter is missing or blank.
 * - 200 OK: at least one [dev.inmo.wishlist.features.deeplinks.server.DeepLinkHandler] matched.
 * - 404 Not Found: deep link unknown or no handler matched.
 *
 * @param feature Business logic delegate for deep link resolution.
 */
class DeepLinksRoutingsConfigurator(
    private val feature: DeepLinksFeature
) : ApplicationRoutingConfigurator.Element {
    /**
     * Installs `GET {linksPrefix}/{deeplinkId}` on the receiver [Route]: validates the id parameter,
     * delegates to [DeepLinksFeature.resolveDeepLink], and maps the outcome to 400/200/404.
     */
    override fun Route.invoke() {
        route(DeepLinksConstants.linksPrefixPathPart) {
            get("{${DeepLinksConstants.deeplinkIdPathParam}}") {
                val rawId = call.parameters[DeepLinksConstants.deeplinkIdPathParam]
                    .takeUnless { it.isNullOrBlank() }
                    ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                val id = DeepLinkId(rawId)
                val handled = feature.resolveDeepLink(id)
                call.respond(if (handled) HttpStatusCode.OK else HttpStatusCode.NotFound)
            }
        }
    }
}
