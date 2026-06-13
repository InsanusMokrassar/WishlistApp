package dev.inmo.wishlist.features.common.client.echo

interface EchoFeature {
    suspend fun getEcho(): String
}
