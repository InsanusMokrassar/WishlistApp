package project_group.project_name.features.sample.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import project_group.project_name.features.sample.common.Constants
import project_group.project_name.features.sample.server.SampleFeature

class SampleRoutingsConfigurator(
    private val sampleFeature: SampleFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        authenticate {
            route(Constants.prefixPathPart) {
                get(Constants.getTextPathPart) {
                    call.respondText(sampleFeature.getSampleText())
                }
            }
        }
    }
}