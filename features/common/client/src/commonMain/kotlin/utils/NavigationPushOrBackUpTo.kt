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

/**
 * Collapses this chain to a single node holding [config]: drops every node above the bottom one
 * (top-down, awaiting each removal) and replaces that bottom node with [config], so the chain ends up
 * holding exactly one node. No-op when the chain already holds a single node satisfying [filter].
 *
 * Unlike [replaceLastOrBackUntil] (which only swaps the top node), this resets the whole chain — used
 * by the sidebar so a click always yields a one-node main chain (a single breadcrumb crumb) regardless
 * of how deep the user had navigated.
 */
suspend inline fun <T> NavigationChain<T>.resetToSingleNode(
    config: T,
    filter: (NavigationNode<*, T>, T) -> Boolean = { node, config -> node.config == config }
) {
    val stack = stackFlow.value
    if (stack.size == 1 && filter(stack.first(), config)) return
    if (stack.isEmpty()) {
        push(config)
        return
    }
    while (stackFlow.value.size > 1) {
        val last = stackFlow.value.last()
        drop(last)
        stackFlow.first { it.last() != last } // waiting for the last element to be removed
    }
    replace(stackFlow.value.first(), config)
}
