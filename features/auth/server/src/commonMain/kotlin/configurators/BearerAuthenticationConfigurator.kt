package project_group.project_name.features.auth.server.configurators

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import project_group.project_name.features.auth.common.models.Token
import project_group.project_name.features.auth.server.services.AuthFeatureService
import project_group.project_name.features.common.server.configurators.ApplicationAuthenticationConfigurator

class BearerAuthenticationConfigurator(
    private val authFeatureService: AuthFeatureService
) : ApplicationAuthenticationConfigurator.Element {
    override fun AuthenticationConfig.invoke() {
        bearer(/*Constants.bearerAuthName*/) {
            authenticate { tokenCredential ->
                val userId = authFeatureService.authenticate(Token(tokenCredential.token))
                if (userId != null) {
                    UserIdPrincipal(userId.long.toString())
                } else {
                    null
                }
            }
        }
    }
}
