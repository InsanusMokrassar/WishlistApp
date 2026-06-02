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

/**
 * Identifier of the scaffold's top navigation chain (the top bar slot).
 *
 * Having a stable id (instead of an anonymous chain) lets the navigation hierarchy be restored
 * from an external source — e.g. the JS URL-based [dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo]
 * — by matching restored sub-chains to the scaffold slots they belong to.
 */
val TopNavigationChainId = NavigationChainId("top")

/**
 * Identifier of the scaffold's left navigation chain (the left column slot).
 *
 * Stable counterpart of [TopNavigationChainId] / [MainNavigationChainId] for the left slot, used so
 * a restored hierarchy can be reattached to the correct scaffold slot.
 */
val LeftNavigationChainId = NavigationChainId("left")
