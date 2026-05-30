package dev.inmo.wishlist.features.ui.scaffold.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * ViewModel for the scaffold layout view.
 *
 * Pure layout container — holds no mutable UI state. Exposes [config] so the platform
 * view can read the three nullable slot configs without storing them separately.
 *
 * @param node navigation node owning this scaffold instance
 */
class ScaffoldViewModel(
    private val node: NavigationNode<ScaffoldViewConfig, ViewConfig>
) : ViewModel<ViewConfig>(node) {

    /** Slot configs for this scaffold instance. */
    val config: ScaffoldViewConfig = node.config
}
