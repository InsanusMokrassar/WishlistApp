package dev.inmo.wishlist.features.common.server.models

import kotlinx.serialization.Serializable

@Serializable
data class KtorConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8083,
    val wss: Boolean = false,
)
