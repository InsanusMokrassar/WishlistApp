package dev.inmo.wishlist.features.email.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.email.common.Constants
import dev.inmo.wishlist.features.email.common.models.SetEmailRequest
import dev.inmo.wishlist.features.email.common.models.TestEmailRequest
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Ktor routing configurator for the email feature, mounted under `/email`.
 *
 * - `POST /email/sendTest` — root-only; sends a fixed test message to the address in the
 *   [TestEmailRequest] body. Non-root callers receive `403 Forbidden`.
 * - `PUT  /email/myEmail`  — any authenticated caller; sets or clears the caller's own stored e-mail
 *   address from the [SetEmailRequest] body.
 *
 * Root verification mirrors `admin` feature gating: the caller must resolve to the `root` username.
 *
 * @param emailFeature Underlying SMTP send capability.
 * @param usersRepo Users CRUD repo used for root verification and for storing the caller's e-mail.
 */
class EmailRoutingsConfigurator(
    private val emailFeature: EmailFeature,
    private val usersRepo: UsersRepo
) : ApplicationRoutingConfigurator.Element {

    private val rootUsername = "root"

    /**
     * Verifies the bearer-authenticated caller is the `root` user.
     *
     * Responds `401` when unauthenticated and `403` when authenticated but not `root`.
     *
     * @return The caller's id when the caller is `root`, otherwise `null` (a response has been sent).
     */
    private suspend fun RoutingContext.requireRoot(): Boolean {
        val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return false
        val user = usersRepo.getById(callerId)
        if (user == null || user.username.string != rootUsername) {
            call.respond(HttpStatusCode.Forbidden)
            return false
        }
        return true
    }

    override fun Route.invoke() {
        authenticate {
            route(Constants.emailPrefixPathPart) {
                post(Constants.sendTestPathPart) {
                    if (!requireRoot()) return@post
                    val request = call.receive<TestEmailRequest>()
                    val sent = emailFeature.sendEmail(
                        to = request.to,
                        subject = "Test email",
                        body = "This is a test email from the WishlistApp email feature."
                    )
                    when (sent) {
                        true -> call.respond(HttpStatusCode.OK)
                        false -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
                put(Constants.myEmailPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val request = call.receive<SetEmailRequest>()
                    val user = usersRepo.getById(callerId) ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@put
                    }
                    val updated = usersRepo.update(
                        callerId,
                        NewUser(username = user.username, email = request.email)
                    )
                    when (updated) {
                        null -> call.respond(HttpStatusCode.InternalServerError)
                        else -> call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
