package dev.inmo.wishlist.features.common.server.echo

interface EchoFeature {
    suspend fun getEcho(): String
}
