package dev.inmo.wishlist.features.simpleRoles.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.simpleRoles.common.Constants
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Ktor routing configurator for the `simpleRoles` feature.
 *
 * Registers a single bearer-authenticated endpoint under `/simpleRoles` (auto-prefixed to
 * `/api/simpleRoles` by the server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET /simpleRoles/isSuperAdmin` — resolves the caller from the bearer token and returns whether
 *   they hold SuperAdmin, via [feature]. `401 Unauthorized` on a missing/invalid token.
 *
 * @param feature Server-side [SimpleRolesFeature] implementation.
 */
class SimpleRolesRoutingsConfigurator(
    private val feature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(Constants.prefixPathPart) {
                get(Constants.isSuperAdminPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@get
                    call.respond(feature.isSuperAdmin(callerId))
                }
            }
        }
    }
}
