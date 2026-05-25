package project_group.project_name.features.common.client.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/**
 * Конфигурирует [HttpTimeout] плагин для клиентов
 *
 * @param connectionTimeouts Таймаут на изначальное подключение к серверу
 * @param requestTimeouts Таймаут на обработку всего запроса в целом
 * @param socketTimeouts Таймаут между двумя пакетами в запросе
 */
class TimeoutsHttpClientConfigurator(
    private val connectionTimeouts: Long = 5000L,
    private val requestTimeouts: Long = 120000L,
    private val socketTimeouts: Long = 20000L,
) : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        install(HttpTimeout) {
            connectTimeoutMillis = connectionTimeouts
            requestTimeoutMillis = requestTimeouts
            socketTimeoutMillis = socketTimeouts
        }
    }
}