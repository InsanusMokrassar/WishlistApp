package dev.inmo.wishlist.features.users.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.users.common.Constants
import dev.inmo.wishlist.features.users.server.UsersFeature
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Registers the single public `GET /users/getAll` endpoint.
 *
 * Endpoint is intentionally not wrapped in `authenticate { }` — any caller can list users.
 *
 * @param feature Underlying [UsersFeature] providing the registered users list.
 */
class UsersRoutingsConfigurator(
    private val feature: UsersFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.usersPrefixPathPart) {
            get(Constants.usersGetAllPathPart) {
                call.respond(feature.getAll())
            }
        }
    }
}
