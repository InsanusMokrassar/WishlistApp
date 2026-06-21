package dev.inmo.wishlist.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import dev.inmo.wishlist.features.common.common.apiPathPart
import io.ktor.http.invoke
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class InternalApplicationRoutingConfigurator(
    private val elements: List<@Contextual ApplicationRoutingConfigurator.Element>
) : KtorApplicationConfigurator {
    private val rootInstaller = ApplicationRoutingConfigurator.Element {
        route(apiPathPart) {
            elements.forEach {
                it.apply { invoke() }
            }
        }
    }

    override fun Application.configure() {
        routing {
            rootInstaller.apply { invoke() }
        }
    }
}