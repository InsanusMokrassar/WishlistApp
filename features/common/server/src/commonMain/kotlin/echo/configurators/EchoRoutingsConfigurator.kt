package project_group.project_name.features.common.server.echo.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import project_group.project_name.features.common.common.echo.EchoConstants
import project_group.project_name.features.common.server.echo.EchoFeature

class EchoRoutingsConfigurator(
    private val echoFeature: EchoFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(EchoConstants.prefixPathPart) {
            get(EchoConstants.getEchoPathPart) {
                call.respondText(echoFeature.getEcho())
            }
        }
    }
}
