package dev.inmo.wishlist.features.common.client.utils

import dev.inmo.navigation.core.repo.ConfigHolder

fun <T, R : T> ConfigHolder<T>.findConfig(filter: (T) -> R?): R? {
    return when (this) {
        is ConfigHolder.Chain<T> -> firstNodeConfig ?.findConfig(filter)
        is ConfigHolder.Node<T> -> {
            filter(config) ?.let { return it }
            subnode ?.findConfig(filter) ?.let { return it }
            subchains.forEach {
                it.findConfig(filter) ?.let { return it }
            }
            null
        }
    }
}
