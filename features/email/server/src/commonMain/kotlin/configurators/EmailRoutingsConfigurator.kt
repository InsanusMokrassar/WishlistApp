package dev.inmo.wishlist.features.email.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.common.models.SetEmailRequest
import dev.inmo.wishlist.features.email.common.models.TestEmailRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
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
 * - `POST /email/sendTest`  — bearer; the caller identity is passed to [feature] which enforces
 *   root-only access.
 * - `PUT  /email/myEmail`   — bearer (self-service); the caller identity is passed to [feature]
 *   which persists the address.
 *
 * The public `enabled` probe lives outside the `authenticate { }` block so callers without a
 * token can still check availability. Authorization and persistence logic reside entirely in
 * [feature] — this configurator only extracts the caller identity and delegates.
 *
 * @param feature Server-side [EmailFeature] implementation that enforces access rules and
 *   handles email-address persistence.
 */
class EmailRoutingsConfigurator(
    private val feature: EmailFeature
) : ApplicationRoutingConfigurator.Element {

    override fun Route.invoke() {
        route(EmailConstants.prefixPathPart) {
            get(EmailConstants.enabledPathPart) {
                call.respond(feature.isFeatureEnabled())
            }
        }

        authenticate {
            route(EmailConstants.prefixPathPart) {
                post(EmailConstants.sendTestPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val request = call.receive<TestEmailRequest>()
                    val sent = feature.sendTestEmail(callerId, request.recipient)
                    when {
                        sent -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                put(EmailConstants.myEmailPathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val request = call.receive<SetEmailRequest>()
                    val updated = feature.setMyEmail(callerId, request.email)
                    when {
                        updated -> call.respond(HttpStatusCode.OK)
                        else -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
    }
}
