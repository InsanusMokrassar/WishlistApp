package dev.inmo.wishlist.features.auth.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.auth.server.ServerAuthFeature
import dev.inmo.wishlist.features.auth.server.configurators.AuthRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.BearerAuthenticationConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.PasswordChangeRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.server.configurators.ApplicationAuthenticationConfigurator

/** Auth server startup plugin wiring routes and the optional Email password-change port. */
/** Auth server startup plugin wiring routes and the optional Email password-change port. */
object Plugin : StartPlugin {
    /** Registers Auth services, routes, serializers, and the optional password-change port. */
    override fun Module.setupDI(config: JsonObject) {
        single { get<Json>().decodeFromJsonElement(Config.serializer(), config) }
        single {
            val config = get<Config>()
            AuthFeatureService(
                usersRepo = get(),
                writeUsersRepo = get(),
                passwordsRepo = get(),
                tokenTtl = config.tokenTtl,
                refreshTokenTtl = config.refreshTokenTtl,
                enableRegistration = config.enableRegistration,
                requireEmailForRegistration = config.requireEmailForRegistration,
                registrationEmailSender = getOrNull<RegistrationEmailSender>(),
                registrationRoleLifecycle = getOrNull<RegistrationRoleLifecycle>(),
                userRoleAuthorization = getOrNull<UserRoleAuthorization>(),
            )
        }
        single<ServerAuthFeature> { get<AuthFeatureService>() }
        single<AuthFeature> { get<AuthFeatureService>() }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            AuthRoutingsConfigurator(get())
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            PasswordChangeRoutingsConfigurator(getOrNull())
        }
        singleWithRandomQualifier<ApplicationAuthenticationConfigurator.Element> {
            BearerAuthenticationConfigurator(get())
        }
    }

    /** Starts Auth server state after service bindings are installed. */
    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
