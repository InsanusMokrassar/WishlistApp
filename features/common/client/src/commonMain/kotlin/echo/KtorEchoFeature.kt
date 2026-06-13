package dev.inmo.wishlist.features.common.client.echo

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import dev.inmo.wishlist.features.common.common.echo.EchoConstants

class KtorEchoFeature(
    private val client: HttpClient
) : EchoFeature {
    override suspend fun getEcho(): String =
        client.get(EchoConstants.fullGetEchoPath).bodyAsText()
}
