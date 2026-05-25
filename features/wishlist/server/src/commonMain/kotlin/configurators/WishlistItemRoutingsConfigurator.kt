package dev.inmo.wishlist.features.wishlist.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.server.services.WishlistItemService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Ktor routing configurator that registers all wishlist item CRUD endpoints under `/wishlistItem`.
 *
 * All routes are wrapped inside an `authenticate {}` block and require a valid bearer token.
 * Mutation routes ([create], [update], [delete]) enforce that the caller owns the parent
 * wishlist; the caller identity is resolved via [getCallerUserIdOrAnswerUnauthorized].
 *
 * Routes registered:
 * - `GET    /wishlistItem/getByWishlistId/{wishlistId}` — returns all items in the given wishlist
 * - `POST   /wishlistItem/create` — creates a new item if caller owns the parent wishlist; body: [NewWishlistItem]
 * - `PUT    /wishlistItem/update/{id}` — replaces item data if caller owns the parent wishlist; body: [NewWishlistItem]
 * - `DELETE /wishlistItem/delete/{id}` — removes an item if caller owns the parent wishlist
 *
 * HTTP status codes for ownership-guarded routes:
 * - `200 OK` — operation succeeded
 * - `403 Forbidden` — item or parent wishlist exists but caller is not the owner
 * - `404 Not Found` — item or parent wishlist not found
 * - `500 Internal Server Error` — create failed (parent not found, not owner, or repo error)
 *
 * @param wishlistItemService Service that enforces ownership and translates requests to repo calls.
 */
class WishlistItemRoutingsConfigurator(
    private val wishlistItemService: WishlistItemService
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(Constants.wishlistItemPrefixPathPart) {
                get("${Constants.wishlistItemGetByWishlistIdPathPart}/{wishlistId}") {
                    val wishlistId = call.parameters["wishlistId"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    call.respond(wishlistItemService.getByWishlistId(wishlistId))
                }
                post(Constants.wishlistItemCreatePathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val newItem = call.receive<NewWishlistItem>()
                    val result = wishlistItemService.create(newItem, callerId)
                    if (result == null) {
                        call.respond(HttpStatusCode.InternalServerError)
                    } else {
                        call.respond(result)
                    }
                }
                put("${Constants.wishlistItemUpdatePathPart}/{id}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    val newItem = call.receive<NewWishlistItem>()
                    when (wishlistItemService.update(id, newItem, callerId)) {
                        true -> call.respond(HttpStatusCode.OK)
                        false -> call.respond(HttpStatusCode.Forbidden)
                        null -> call.respond(HttpStatusCode.NotFound)
                    }
                }
                delete("${Constants.wishlistItemDeletePathPart}/{id}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@delete
                    val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    when (wishlistItemService.delete(id, callerId)) {
                        true -> call.respond(HttpStatusCode.OK)
                        false -> call.respond(HttpStatusCode.Forbidden)
                        null -> call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}
