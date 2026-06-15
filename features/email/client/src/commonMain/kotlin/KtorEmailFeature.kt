package dev.inmo.wishlist.features.email.client

import dev.inmo.wishlist.features.email.common.Constants
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.SetEmailRequest
import dev.inmo.wishlist.features.email.common.models.TestEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

/**
 * Ktor HTTP implementation of [EmailFeature]. Only performs HTTP calls and reports success — no extra
 * logic, per project convention.
 *
 * @param client Shared HTTP client (carries bearer auth and default server URL).
 */
class KtorEmailFeature(
    private val client: HttpClient
) : EmailFeature {
    private val basePath = Constants.emailPrefixPathPart

    override suspend fun sendTestEmail(to: Email): Boolean {
        val response = client.post("$basePath/${Constants.sendTestPathPart}") {
            setBody(TestEmailRequest(to))
        }
        return response.status.isSuccess()
    }

    override suspend fun setMyEmail(email: Email?): Boolean {
        val response = client.put("$basePath/${Constants.myEmailPathPart}") {
            setBody(SetEmailRequest(email))
        }
        return response.status.isSuccess()
    }
}
