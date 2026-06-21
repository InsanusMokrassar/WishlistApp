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

/**
 * [HttpClientConfigurator] that points every outgoing request at the user-configured server.
 *
 * Installs a client plugin that, per request, reads the stored server URL and fills the request's
 * absent URL parts (scheme, host, port, path, ...) from it. When [useDefaultUrlPrefix] is set it also
 * guarantees the base URL begins with the `/api` path prefix, so bare feature paths resolve under the
 * server's `/api` routing root.
 *
 * @param storage Source of the user-configured server URL.
 * @param useDefaultUrlPrefix Whether to prepend the `/api` path prefix to the base URL (default `true`);
 * disable when the configured URL already targets the API root.
 */
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
                // Ensure the base URL carries `/api` as the FIRST path segment (the server mounts every
                // route under `route("api")` at the routing root). Parse the URL first so an existing
                // path / query / fragment is preserved rather than matched by a fragile string suffix;
                // skip when disabled or when `/api` is already the leading segment.
                val fixedCurrentUrl = when {
                    useDefaultUrlPrefix.not() -> schemeFixedUrl
                    else -> {
                        val builder = URLBuilder(schemeFixedUrl)
                        val pathSegments = builder.encodedPathSegments.filter { it.isNotEmpty() }
                        when (pathSegments.firstOrNull()) {
                            apiPathPart -> schemeFixedUrl
                            else -> {
                                builder.encodedPathSegments = listOf(apiPathPart) + pathSegments
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
