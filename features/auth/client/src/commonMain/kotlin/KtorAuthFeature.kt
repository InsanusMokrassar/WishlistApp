package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.LoginRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshRequest
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.RegisterRequest
import dev.inmo.wishlist.features.users.common.models.Username

class KtorAuthFeature(
    private val client: HttpClient
) : ClientAuthFeature {
    private val loginPath = "${Constants.prefixPathPart}/${Constants.loginPathPart}"
    private val refreshPath = "${Constants.prefixPathPart}/${Constants.refreshPathPart}"
    private val logoutPath = "${Constants.prefixPathPart}/${Constants.logoutPathPart}"
    private val getMePath = "${Constants.prefixPathPart}/${Constants.getMePathPart}"
    private val registerPath = "${Constants.prefixPathPart}/${Constants.registerPathPart}"
    private val isRegistrationAvailablePath = "${Constants.prefixPathPart}/${Constants.isRegistrationAvailablePathPart}"

    override suspend fun login(username: Username, password: Password): AuthCredentials? {
        val response: HttpResponse = client.post(loginPath) {
            setBody(LoginRequest(username, password))
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? {
        val response: HttpResponse = client.post(refreshPath) {
            setBody(RefreshRequest(refreshToken))
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun logout() {
        client.post(logoutPath)
    }

    override suspend fun register(username: Username, password: Password): AuthCredentials? {
        val response: HttpResponse = client.post(registerPath) {
            setBody(RegisterRequest(username, password))
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun isRegistrationAvailable(): Boolean = runCatchingLogging {
        val response = client.get(isRegistrationAvailablePath)
        if (!response.status.isSuccess()) return@runCatchingLogging false
        response.body<Boolean>()
    }.getOrDefault(false)

    override suspend fun getMe(): AuthFeatureUser? {
        val response = runCatchingLogging {
            client.get(getMePath) {
                expectSuccess = true
            }
        }.onFailure {
            if (it is ClientRequestException && it.response.status == HttpStatusCode.Unauthorized) {
                return null
            }
        }.getOrThrow()
        return if (response.status.isSuccess()) response.body() else null
    }
}
