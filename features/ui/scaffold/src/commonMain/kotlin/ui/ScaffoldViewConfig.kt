package dev.inmo.wishlist.features.ui.scaffold.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * Navigation config for the scaffold layout view.
 *
 * Carries up to three optional sub-configs, each bootstrapping its own navigation chain.
 *
 * @param topConfig optional [ViewConfig] for the top slot; spans full width
 * @param leftConfig optional [ViewConfig] for the left slot; spans height below the top slot
 * @param mainConfig optional [ViewConfig] for the main content area; takes remaining space
 */
@Serializable
class ScaffoldViewConfig(
    @Polymorphic val topConfig: ViewConfig? = null,
    @Polymorphic val leftConfig: ViewConfig? = null,
    @Polymorphic val mainConfig: ViewConfig? = null
) : ViewConfig
