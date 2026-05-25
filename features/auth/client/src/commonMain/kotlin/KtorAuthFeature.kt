package project_group.project_name.features.auth.client

import dev.inmo.micro_utils.common.Either
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import project_group.project_name.features.auth.common.Constants
import project_group.project_name.features.auth.common.models.AuthCredentials
import project_group.project_name.features.auth.common.models.LoginRequest
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.auth.common.models.RefreshRequest
import project_group.project_name.features.auth.common.models.RefreshToken
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.Username

class KtorAuthFeature(
    private val client: HttpClient
) : ClientAuthFeature {
    private val loginPath = "${Constants.prefixPathPart}/${Constants.loginPathPart}"
    private val refreshPath = "${Constants.prefixPathPart}/${Constants.refreshPathPart}"
    private val logoutPath = "${Constants.prefixPathPart}/${Constants.logoutPathPart}"
    private val getMePath = "${Constants.prefixPathPart}/${Constants.getMePathPart}"

    override suspend fun login(username: Username, password: Password): AuthCredentials? {
        val response: HttpResponse = client.post(loginPath) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? {
        val response: HttpResponse = client.post(refreshPath) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun logout() {
        client.post(logoutPath)
    }

    override suspend fun getMe(): RegisteredUser? {
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
