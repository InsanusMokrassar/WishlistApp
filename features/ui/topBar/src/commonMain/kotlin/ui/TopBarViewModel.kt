package dev.inmo.wishlist.features.ui.topBar.ui

import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/**
 * ViewModel for the top bar.
 *
 * Holds no mutable state — only forwards "change server URL" intent to [interactor].
 * The embedded auth widget owns its own state via [dev.inmo.wishlist.features.ui.auth].
 *
 * @param node Navigation node owning the top bar.
 * @param interactor Navigation delegate for top-level actions (open server URL editor).
 */
class TopBarViewModel(
    private val node: NavigationNode<TopBarViewConfig, ViewConfig>,
    private val interactor: TopBarViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** Forwards "change server URL" to [TopBarViewInteractor.onChangeServerUrl]. */
    fun onChangeServerUrl() {
        scope.launchLoggingDropExceptions { interactor.onChangeServerUrl(node) }
    }
}
