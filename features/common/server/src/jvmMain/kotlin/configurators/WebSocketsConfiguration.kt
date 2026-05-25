package project_group.project_name.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

class WebSocketsConfiguration(
    private val json: Json
) : KtorApplicationConfigurator {
    override fun Application.configure() {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }
}
