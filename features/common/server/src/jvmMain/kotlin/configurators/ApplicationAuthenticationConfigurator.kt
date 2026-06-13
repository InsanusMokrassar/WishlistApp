package dev.inmo.wishlist.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authentication

class ApplicationAuthenticationConfigurator(
    private val elements: List<Element>
) : KtorApplicationConfigurator {
    fun interface Element { operator fun AuthenticationConfig.invoke() }

    override fun Application.configure() {
        authentication {
            elements.forEach { it.apply { invoke() } }
        }
    }
}
