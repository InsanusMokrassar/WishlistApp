package dev.inmo.wishlist.features.ui.serverUrl.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Side-effecting capability for the server URL editor.
 *
 * Implementation lives in the top-level `client/` module — it decides how the
 * root chain advances after a successful save.
 */
interface ServerUrlViewInteractor {
    /**
     * Invoked when the user has saved a valid URL.
     *
     * @param node Navigation node hosting the server URL view.
     */
    suspend fun onSaved(node: NavigationNode<ServerUrlViewConfig, ViewConfig>)
}
