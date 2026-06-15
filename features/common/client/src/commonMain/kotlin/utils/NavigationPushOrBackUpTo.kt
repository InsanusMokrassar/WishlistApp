package dev.inmo.wishlist.features.common.client.utils

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

suspend inline fun <T> NavigationChain<T>.actionOrBackUntil(
    config: T,
    filter: (NavigationNode<*, T>, T) -> Boolean = { node, config -> node.config == config },
    action: NavigationChain<T>.(config: T) -> Unit
) {
    val nodeToLeft = stackFlow.value.firstOrNull { filter(it, config) }
    if (nodeToLeft != null) {
        var last = stackFlow.value.last()
        while (last !== nodeToLeft) {
            drop(last)
            stackFlow.first { it.last() != last } // waiting for the last element to be removed
            last = stackFlow.value.last()
        }
    } else {
        action(config)
    }
}

suspend inline fun <T> NavigationChain<T>.pushOrBackUntil(
    config: T,
    filter: (NavigationNode<*, T>, T) -> Boolean = { node, config -> node.config == config }
) {
    actionOrBackUntil(config, filter) { push(config) }
}

suspend inline fun <T> NavigationChain<T>.replaceLastOrBackUntil(
    config: T,
    filter: (NavigationNode<*, T>, T) -> Boolean = { node, config -> node.config == config }
) {
    actionOrBackUntil(config, filter) { replace(stackFlow.value.last(), config) }
}
