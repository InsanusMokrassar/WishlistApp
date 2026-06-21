package dev.inmo.wishlist.features.email.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.common.EmailFeature
import dev.inmo.wishlist.features.email.common.models.SetEmailRequest
import dev.inmo.wishlist.features.email.common.models.TestEmailRequest
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Ktor routing configurator for the email feature.
 *
 * Registers three endpoints under the `/email` path prefix (auto-prefixed to `/api/email` by the
 * server's `InternalApplicationRoutingConfigurator`):
 *
 * - `GET  /email/enabled`   — public; returns whether SMTP delivery is configured.
 * - `POST /email/sendTest`  — bearer + root-only; delivers a test message to the supplied address.
 * - `PUT  /email/myEmail`   — bearer (self-service); stores or clears the caller's own email.
 *
 * The public `enabled` probe lives outside the `authenticate { }` block so callers without a token
 * can still check availability. The two mutating endpoints are wrapped in `authenticate { }`.
 *
 * @param feature Server-side [EmailFeature] implementation (SmtpEmailService).
 * @param usersRepo Repository used to read and update per-user email addresses.
 */
class EmailRoutingsConfigurator(
    private val feature: EmailFeature,
    private val usersRepo: UsersRepo
) : ApplicationRoutingConfigurator.Element {

    /** Username that identifies the privileged root account. */
    private val rootUsername = "root"

    /**
     * Verifies the bearer token caller is the root user.
     *
     * Responds with `401 Unauthorized` when no valid token is present, or `403 Forbidden` when the
     * authenticated caller is not root.
     *
     * @return The root [UserId] on success, or `null` after an error response has been sent.
     */
    private suspend fun RoutingContext.requireRoot(): UserId? {
        val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
        val user = usersRepo.getById(callerId)
        if (user == null || user.username.string != rootUsername) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }
        return callerId
    }

    override fun Route.invoke() {
        // Public probe — no authentication required.
        route(EmailConstants.prefixPathPart) {
            get(EmailConstants.enabledPathPart) {
                call.respond(feature.isFeatureEnabled())
            }
        }

        // Authenticated endpoints.
        authenticate {
            route(EmailConstants.prefixPathPart) {
                post(EmailConstants.sendTestPathPart) {
                    requireRoot() ?: return@post
                    val request = call.receive<TestEmailRequest>()
                    val sent = feature.sendTestEmail(request.recipient)
                    when {
                        sent -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                put(EmailConstants.myEmailPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val request = call.receive<SetEmailRequest>()
                    val user = usersRepo.getById(callerId) ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@put
                    }
                    val updated = usersRepo.update(callerId, NewUser(user.username, request.email))
                    when {
                        updated != null -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
    }
}
