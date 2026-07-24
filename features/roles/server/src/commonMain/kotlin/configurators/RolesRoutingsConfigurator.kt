package dev.inmo.wishlist.features.roles.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.roles.common.RolesConstants
import dev.inmo.wishlist.features.roles.server.RolesFeature
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Ktor routing configurator for the `roles` feature.
 *
 * Registers a single bearer-authenticated endpoint under `/roles` (auto-prefixed to `/api/roles` by
 * the server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET /roles/isFunctionalityAvailable/{functionalityId}` — resolves the caller from the bearer
 *   token and returns whether that caller may access the given functionality, via [feature]. `401`
 *   on a missing/invalid token, `400` when the functionality id path segment is missing.
 *
 * @param feature Server-side [RolesFeature] implementation.
 */
class RolesRoutingsConfigurator(
    private val feature: RolesFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(RolesConstants.prefixPathPart) {
                get("${RolesConstants.isFunctionalityAvailablePathPart}/{${RolesConstants.functionalityIdParameter}}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@get
                    val functionalityId = call.parameters[RolesConstants.functionalityIdParameter]
                        ?.let { FunctionalityId(it) }
                    if (functionalityId == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    call.respond(feature.isFunctionalityAvailable(callerId, functionalityId))
                }
            }
        }
    }
}
