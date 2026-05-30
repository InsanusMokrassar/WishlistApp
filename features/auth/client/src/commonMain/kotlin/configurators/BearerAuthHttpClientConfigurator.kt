package dev.inmo.wishlist.features.auth.client.configurators

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.RefreshRequest
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.common.client.configurators.HttpClientConfigurator

class BearerAuthHttpClientConfigurator(
    private val storage: AuthCredentialsStorage,
) : HttpClientConfigurator {
    private val refreshPath = "${Constants.prefixPathPart}/${Constants.refreshPathPart}"
    private val loginPath = "${Constants.prefixPathPart}/${Constants.loginPathPart}"
    private val registerPath = "${Constants.prefixPathPart}/${Constants.registerPathPart}"
    private val isRegistrationAvailablePath = "${Constants.prefixPathPart}/${Constants.isRegistrationAvailablePathPart}"

    override fun HttpClientConfig<*>.configure() {
        val storage = storage
        val refreshPath = refreshPath
        val loginPath = loginPath
        val registerPath = registerPath
        val configPath = isRegistrationAvailablePath
        install(Auth) {
            bearer {
                cacheTokens = false
                loadTokens {
                    val credentials = storage.get()
                    credentials ?: return@loadTokens null
                    BearerTokens(credentials.token.string, credentials.refreshToken.string)
                }
                refreshTokens {
                    val oldRefresh = oldTokens ?.refreshToken ?: storage.get()?.refreshToken?.string
                    if (oldRefresh == null) return@refreshTokens null
                    val response: HttpResponse? = runCatchingLogging {
                        client.post(refreshPath) {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(RefreshToken(oldRefresh)))
                        }
                    }.getOrNull()
                    val newCredentials: AuthCredentials = if (response != null && response.status.isSuccess()) {
                        response.body()
                    } else {
                        storage.save(null)
                        return@refreshTokens null
                    }
                    storage.save(newCredentials)
                    BearerTokens(newCredentials.token.string, newCredentials.refreshToken.string)
                }
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath
                    !path.endsWith(loginPath) &&
                        !path.endsWith(refreshPath) &&
                        !path.endsWith(registerPath) &&
                        !path.endsWith(configPath)
                }
            }
        }
    }
}
