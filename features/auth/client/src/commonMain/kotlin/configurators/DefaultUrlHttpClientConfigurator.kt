package dev.inmo.wishlist.features.auth.client.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLBuilder
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.common.client.configurators.HttpClientConfigurator
import dev.inmo.wishlist.features.common.client.utils.appendOrSetPartsWith
import dev.inmo.wishlist.features.common.client.utils.fillAbsentPartsWith
import dev.inmo.wishlist.features.common.client.utils.set
import dev.inmo.wishlist.features.common.common.apiPathPart

class DefaultUrlHttpClientConfigurator(
    private val storage: ServerUrlStorage,
    private val useDefaultUrlPrefix: Boolean = true
) : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        val storage = storage
        val plugin = createClientPlugin("DefaultServerUrlPlugin") {
            onRequest { request, _ ->
                val currentUrl = storage.getServerUrl() ?: return@onRequest
                val schemeFixedUrl = if (currentUrl.contains("://")) {
                    currentUrl
                } else {
                    "http://$currentUrl"
                }
                // Add the `/api` prefix to the base URL by appending it as a path segment, parsing the
                // URL first so an existing path / query / fragment is preserved and not matched by a
                // fragile string suffix. Skip when disabled or when `/api` is already the last segment.
                val fixedCurrentUrl = when {
                    useDefaultUrlPrefix.not() -> schemeFixedUrl
                    else -> {
                        val builder = URLBuilder(schemeFixedUrl)
                        val pathSegments = builder.encodedPathSegments.filter { it.isNotEmpty() }
                        when (pathSegments.lastOrNull()) {
                            apiPathPart -> schemeFixedUrl
                            else -> {
                                builder.encodedPathSegments = listOf("") + pathSegments + apiPathPart
                                builder.buildString()
                            }
                        }
                    }
                }
                val newUrlBuilder = URLBuilder(fixedCurrentUrl)
                newUrlBuilder.appendOrSetPartsWith(request.url)
                request.url.set(newUrlBuilder)
            }
        }
        install(plugin)
    }
}
