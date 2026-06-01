package dev.inmo.wishlist.features.common.client.models

import dev.inmo.navigation.core.NavigationChainId

/**
 * Identifier of the scaffold's main navigation chain (the central content area).
 *
 * Assigned to the `mainConfig` [dev.inmo.navigation.compose.InjectNavigationChain] inside the
 * scaffold so other features (e.g. the top bar) can locate that chain in the navigation subtree
 * and inspect its stack — for instance to read the currently shown screen's title.
 */
val MainNavigationChainId = NavigationChainId("main")
