package dev.inmo.wishlist.features.wishlist.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.models.CopyWishlistRequest
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.server.services.WishlistCopyService
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
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
 * Ktor routing configurator that registers all wishlist CRUD endpoints under `/wishlist`.
 *
 * **Public routes** (no auth required):
 * - `GET /wishlist/getByUserId/{userId}` — returns all wishlists for the given user
 * - `GET /wishlist/getById/{id}` — returns a single wishlist by id
 *
 * **Auth-required routes** (valid bearer token mandatory):
 * - `GET  /wishlist/getMy` — returns all wishlists owned by the authenticated caller
 * - `POST /wishlist/create` — creates a new wishlist for the caller; body: [NewWishlistInFeature]
 * - `POST /wishlist/copy` — enqueues a background deep-copy of a wishlist into the caller's profile; body: [CopyWishlistRequest]
 * - `PUT  /wishlist/update/{id}` — replaces wishlist data if caller is owner; body: [NewWishlistInFeature]
 * - `DELETE /wishlist/delete/{id}` — removes a wishlist if caller is owner
 *
 * HTTP status codes for ownership-guarded routes:
 * - `200 OK` — operation succeeded
 * - `202 Accepted` — wishlist-copy job queued for background processing
 * - `403 Forbidden` — wishlist exists but caller is not the owner
 * - `404 Not Found` — no wishlist with the given id
 *
 * @param wishlistService Service that enforces ownership and translates requests to repo calls.
 * @param wishlistCopyService Background queue service that processes whole-wishlist copy jobs.
 */
class WishlistRoutingsConfigurator(
    private val wishlistService: WishlistService,
    private val wishlistCopyService: WishlistCopyService
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.wishlistPrefixPathPart) {
            get("${Constants.wishlistGetByUserIdPathPart}/{userId}") {
                val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                call.respond(wishlistService.getByUserId(userId))
            }
            get("${Constants.wishlistGetByIdPathPart}/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val wishlist = wishlistService.getById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(wishlist)
            }
        }
        authenticate {
            route(Constants.wishlistPrefixPathPart) {
                get(Constants.wishlistGetMyPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@get
                    call.respond(wishlistService.getMyWishlists(callerId))
                }
                post(Constants.wishlistCreatePathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val newWishlist = call.receive<NewWishlistInFeature>()
                    val result = wishlistService.create(newWishlist, callerId)
                    if (result == null) {
                        call.respond(HttpStatusCode.InternalServerError)
                    } else {
                        call.respond(result)
                    }
                }
                post(Constants.wishlistCopyPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val request = call.receive<CopyWishlistRequest>()
                    val job = wishlistCopyService.enqueue(request.sourceWishlistId, callerId)
                    if (job == null) {
                        call.respond(HttpStatusCode.InternalServerError)
                    } else {
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                put("${Constants.wishlistUpdatePathPart}/{id}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    val newWishlist = call.receive<NewWishlistInFeature>()
                    when (wishlistService.update(id, newWishlist, callerId)) {
                        true -> call.respond(HttpStatusCode.OK)
                        false -> call.respond(HttpStatusCode.Forbidden)
                        null -> call.respond(HttpStatusCode.NotFound)
                    }
                }
                delete("${Constants.wishlistDeletePathPart}/{id}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@delete
                    val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    when (wishlistService.delete(id, callerId)) {
                        true -> call.respond(HttpStatusCode.OK)
                        false -> call.respond(HttpStatusCode.Forbidden)
                        null -> call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}
