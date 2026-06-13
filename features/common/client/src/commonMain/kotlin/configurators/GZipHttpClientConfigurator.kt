package dev.inmo.wishlist.features.common.client.configurators

import io.ktor.client.*
import io.ktor.client.plugins.compression.*

class GZipHttpClientConfigurator : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        install(ContentEncoding) {
            gzip()
        }
    }
}