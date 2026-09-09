package dev.inmo.wishlist.features.auth.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException

/**
 * Routes Auth's public password-change transport to Email's optional server port.
 *
 * Request issuance requires the normal bearer route. Completion is deliberately outside
 * authentication because the persisted email approval, rather than browser login state, selects and
 * authorizes the password subject.
 *
 * @param feature Optional Email-owned implementation; missing infrastructure fails closed.
 */
class PasswordChangeRoutingsConfigurator(
    private val feature: ServerPasswordChangeFeature?,
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.prefixPathPart) {
            authenticate {
                post(Constants.requestPasswordChangeEmailPathPart) {
                    call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                    call.response.headers.append("Referrer-Policy", "no-referrer")
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val request = try {
                        call.receive<PasswordChangeEmailRequest>()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val result = try {
                        feature?.requestPasswordChangeEmail(callerId, request.expectedEmail)
                            ?: PasswordChangeEmailRequestResult.Unavailable
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        call.respond(HttpStatusCode.InternalServerError)
                        return@post
                    }
                    call.respond(result)
                }
            }

            post(Constants.completePasswordChangePathPart) {
                call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                call.response.headers.append("Referrer-Policy", "no-referrer")
                val request = try {
                    call.receive<CompletePasswordChangeRequest>()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val result = try {
                    feature?.completePasswordChange(request) ?: PasswordChangeResult.InvalidApproval
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }
                call.respond(result)
            }
        }
    }
}
