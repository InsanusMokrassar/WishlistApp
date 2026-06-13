package dev.inmo.wishlist.features.common.client.models

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.Serializable

@Serializable
class EmptyConfig : ViewConfig {
    override fun toString(): String {
        return "EmptyConfig"
    }
}
