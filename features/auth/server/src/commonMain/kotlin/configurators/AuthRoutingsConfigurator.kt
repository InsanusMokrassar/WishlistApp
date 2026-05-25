package dev.inmo.wishlist.features.auth.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.auth.common.models.LoginRequest
import dev.inmo.wishlist.features.auth.common.models.RefreshRequest
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.ServerAuthFeature

class AuthRoutingsConfigurator(
    private val authFeature: ServerAuthFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.prefixPathPart) {
            post(Constants.loginPathPart) {
                val request = call.receive<LoginRequest>()
                val credentials = authFeature.login(request.username, request.password)
                if (credentials == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    call.respond(credentials)
                }
            }
            post(Constants.refreshPathPart) {
                val request = call.receive<RefreshRequest>()
                val credentials = authFeature.refresh(request.refreshToken)
                if (credentials == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    call.respond(credentials)
                }
            }
            authenticate {
                post(Constants.logoutPathPart) {
                    val header = call.request.parseAuthorizationHeader() ?: run {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    if (header.authScheme != "Bearer") {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    authFeature.logout(Token(header.render().removePrefix(header.authScheme).trim()))
                    call.respond(HttpStatusCode.OK)
                }
                get(Constants.getMePathPart) {
                    val header = call.request.parseAuthorizationHeader() ?: run {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }
                    if (header.authScheme != "Bearer") {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val token = Token(header.render().removePrefix(header.authScheme).trim())
                    val registeredUser = authFeature.getUser(token)
                    if (registeredUser == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                    } else {
                        call.respond(registeredUser)
                    }
                }
            }
        }
    }
}
