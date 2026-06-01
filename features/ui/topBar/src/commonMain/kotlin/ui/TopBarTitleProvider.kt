package dev.inmo.wishlist.features.ui.topBar.ui

import androidx.compose.runtime.Composable

/**
 * Implemented by navigation nodes (views) that want to drive the top bar title.
 *
 * The top bar locates the scaffold's main navigation chain and concatenates the [title] of every
 * node in that chain's stack which implements this interface (in stack order, separated by " / ").
 * Because [title] is a `@Composable` getter, implementations may derive it from their own
 * observable state (e.g. a loaded entity name).
 */
interface TopBarTitleProvider {
    /** Title segment contributed to the top bar while this node is on the main chain's stack. */
    val title: String
        @Composable get
}
