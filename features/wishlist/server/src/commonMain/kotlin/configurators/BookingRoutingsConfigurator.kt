package dev.inmo.wishlist.features.wishlist.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.wishlist.common.Constants
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.server.services.BookResult
import dev.inmo.wishlist.features.wishlist.server.services.BookingResult
import dev.inmo.wishlist.features.wishlist.server.services.BookingService
import dev.inmo.wishlist.features.wishlist.server.services.CancelResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Ktor routing configurator registering the wishlist-item booking endpoints under
 * `/wishlistItemBooking`.
 *
 * **All routes require a valid bearer token** — the whole tree is wrapped in `authenticate { }`,
 * so anonymous callers receive `401 Unauthorized` and never reach the service (rule 2: only
 * authorized users access the booking feature).
 *
 * - `GET  /wishlistItemBooking/state/{itemId}` — returns [dev.inmo.wishlist.features.wishlist.common.models.BookingState]
 * - `POST /wishlistItemBooking/book/{itemId}` — reserves the item for the caller
 * - `POST /wishlistItemBooking/cancel/{itemId}` — cancels the caller's own reservation
 *
 * HTTP status semantics:
 * - `200 OK` — operation succeeded (booking state body on `state`)
 * - `400 Bad Request` — `{itemId}` is not a valid Long
 * - `401 Unauthorized` — no/invalid bearer token
 * - `403 Forbidden` — caller owns the item (booking state hidden from owners, rule 3) or, for
 *   `cancel`, the existing booking belongs to a different user
 * - `404 Not Found` — item or its parent wishlist does not exist
 * - `409 Conflict` — `book` attempted on an already-booked item (single-booking, rule 4)
 *
 * @param bookingService Service enforcing all booking rules server-side.
 */
class BookingRoutingsConfigurator(
    private val bookingService: BookingService
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(Constants.bookingPrefixPathPart) {
                get("${Constants.bookingStatePathPart}/{itemId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@get
                    val itemId = call.parameters["itemId"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    when (val result = bookingService.getState(itemId, callerId)) {
                        BookingResult.ItemNotFound -> call.respond(HttpStatusCode.NotFound)
                        BookingResult.OwnerForbidden -> call.respond(HttpStatusCode.Forbidden)
                        is BookingResult.State -> call.respond(result.state)
                    }
                }
                post("${Constants.bookingBookPathPart}/{itemId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val itemId = call.parameters["itemId"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    when (bookingService.book(itemId, callerId)) {
                        BookResult.ItemNotFound -> call.respond(HttpStatusCode.NotFound)
                        BookResult.OwnerForbidden -> call.respond(HttpStatusCode.Forbidden)
                        BookResult.AlreadyBooked -> call.respond(HttpStatusCode.Conflict)
                        BookResult.Ok -> call.respond(HttpStatusCode.OK)
                    }
                }
                post("${Constants.bookingCancelPathPart}/{itemId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val itemId = call.parameters["itemId"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    when (bookingService.cancel(itemId, callerId)) {
                        CancelResult.ItemNotFound -> call.respond(HttpStatusCode.NotFound)
                        CancelResult.OwnerForbidden -> call.respond(HttpStatusCode.Forbidden)
                        CancelResult.NotBooker -> call.respond(HttpStatusCode.Forbidden)
                        CancelResult.Ok -> call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
