package dev.inmo.wishlist.features.common.client.configurators

import dev.inmo.kslog.common.invoke
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SerializationConfigurator(private val json: Json) : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}