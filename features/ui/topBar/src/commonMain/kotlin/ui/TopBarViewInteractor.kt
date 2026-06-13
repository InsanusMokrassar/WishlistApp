package dev.inmo.wishlist.features.ui.topBar.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * Side-effecting capability for the top bar — opens the server URL editor.
 *
 * Implementation lives in the top-level `client/` module; it operates on the
 * root navigation chain, NOT the top bar's own chain.
 */
interface TopBarViewInteractor {
    /**
     * Invoked when the user taps the "Change server URL" button.
     *
     * @param node Navigation node hosting the top bar view.
     */
    suspend fun onChangeServerUrl(
        node: NavigationNode<TopBarViewConfig, ViewConfig>
    )
}
