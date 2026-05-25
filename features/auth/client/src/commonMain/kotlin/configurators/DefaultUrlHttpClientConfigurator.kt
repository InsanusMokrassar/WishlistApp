package project_group.project_name.features.auth.client.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLBuilder
import project_group.project_name.features.auth.client.ServerUrlStorage
import project_group.project_name.features.common.client.configurators.HttpClientConfigurator
import project_group.project_name.features.common.client.utils.fillAbsentPartsWith

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
                request.url.fillAbsentPartsWith(newUrlBuilder)
            }
        }
        install(plugin)
    }
}
