package dev.inmo.wishlist.features.auth.client.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLBuilder
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.common.client.configurators.HttpClientConfigurator
import dev.inmo.wishlist.features.common.client.utils.appendOrSetPartsWith
import dev.inmo.wishlist.features.common.client.utils.fillAbsentPartsWith
import dev.inmo.wishlist.features.common.client.utils.set

class DefaultUrlHttpClientConfigurator(
    private val storage: ServerUrlStorage
) : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        val storage = storage
        val plugin = createClientPlugin("DefaultServerUrlPlugin") {
            onRequest { request, _ ->
                val currentUrl = storage.getServerUrl() ?: return@onRequest
                val fixedCurrentUrl = if (currentUrl.contains("://") == false) "http://$currentUrl" else currentUrl
                val newUrlBuilder = URLBuilder(fixedCurrentUrl)
                newUrlBuilder.appendOrSetPartsWith(request.url)
                request.url.set(newUrlBuilder)
            }
        }
        install(plugin)
    }
}
