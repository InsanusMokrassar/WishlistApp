package project_group.project_name.features.common.client.configurators

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface HttpClientConfigurator {
    fun HttpClientConfig<*>.configure()
}