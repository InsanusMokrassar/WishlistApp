package dev.inmo.wishlist.features.common.server.echo.services

import dev.inmo.wishlist.features.common.server.echo.EchoFeature

class SimpleEchoFeatureService(
    private val responseText: String = "Echo from server: it works!"
) : EchoFeature {
    override suspend fun getEcho(): String = responseText
}
