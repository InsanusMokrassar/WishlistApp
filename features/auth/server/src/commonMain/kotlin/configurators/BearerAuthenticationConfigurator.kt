package dev.inmo.wishlist.features.auth.server.configurators

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.server.configurators.ApplicationAuthenticationConfigurator

class BearerAuthenticationConfigurator(
    private val authFeatureService: AuthFeatureService
) : ApplicationAuthenticationConfigurator.Element {
    override fun AuthenticationConfig.invoke() {
        bearer(/*Constants.bearerAuthName*/) {
            authenticate { tokenCredential ->
                val userId = authFeatureService.authenticate(Token(tokenCredential.token))
                userId
            }
        }
    }
}
