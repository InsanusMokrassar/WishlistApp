package project_group.project_name.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

class ContentNegotiationKtorApplicationConfigurator(
    val jsonFormat: Json
) : KtorApplicationConfigurator {
    override fun Application.configure() {
        install(ContentNegotiation) {
            json(jsonFormat)
        }
    }
}
